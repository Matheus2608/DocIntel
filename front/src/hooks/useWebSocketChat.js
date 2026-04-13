import { useState, useEffect, useRef } from 'react';
import { API_BASE_URL } from '../config';

/**
 * Hook para gerenciar WebSocket e mensagens do chat
 */
export const useWebSocketChat = (chatId, wsUrl) => {
    const [messages, setMessages] = useState([]);
    const [isConnected, setIsConnected] = useState(false);
    const [isTyping, setIsTyping] = useState(false);
    const [liveStep, setLiveStep] = useState(null);

    const wsRef = useRef(null);
    const currentStreamRef = useRef(null);
    const reconnectDelayRef = useRef(1000);
    const pendingStepsRef = useRef([]);

    const sanitizeSteps = (steps) =>
        (steps ?? []).map(s => s.status === 'running' ? { ...s, status: 'done' } : s);

    const sanitizeLastAssistantSteps = (list) => {
        let idx = -1;
        for (let i = list.length - 1; i >= 0; i--) {
            if (list[i].role === 'assistant') { idx = i; break; }
        }
        if (idx < 0 || !Array.isArray(list[idx].steps)) return list;
        const copy = [...list];
        copy[idx] = { ...copy[idx], steps: sanitizeSteps(copy[idx].steps) };
        return copy;
    };

    const safeParseJson = (raw) => {
        try { return JSON.parse(raw); } catch { return null; }
    };

    const buildStep = (event) => ({
        tool: event.tool,
        status: event.status === 'start' ? 'running' : 'done',
        arguments: event.arguments ?? null,
        startedAt: Date.now(),
    });

    const updateSteps = (steps, event) => {
        if (event.status === 'start') {
            return [...steps, buildStep(event)];
        }

        if (event.status === 'end') {
            let updated = false;
            const next = steps.map((step) => {
                if (!updated && step.tool === event.tool && step.status === 'running') {
                    updated = true;
                    return { ...step, status: 'done' };
                }
                return step;
            });
            return updated ? next : [...next, { ...buildStep(event), status: 'done' }];
        }

        return steps;
    };

    // Carrega mensagens anteriores do chat
    const loadPastMessages = async (chatId) => {
        try {
            const response = await fetch(`${API_BASE_URL}/${chatId}/messages`);
            if (response.ok) {
                const pastMessages = await response.json();
                const formattedMessages = pastMessages.map(msg => ({
                    id: msg.id,
                    createdAt: msg.createdAt,
                    role: msg.role,
                    text: msg.content,
                    stepCount: msg.stepCount ?? 0,
                }));
                setMessages(formattedMessages);
            }
        } catch (error) {
            console.warn('Erro ao carregar mensagens:', error);
        }
    };

    // Busca os passos persistidos de uma mensagem (para histórico)
    const fetchSteps = async (messageId) => {
        if (!chatId || !messageId) return [];
        try {
            const response = await fetch(`${API_BASE_URL}/${chatId}/messages/${messageId}/steps`);
            if (!response.ok) return [];
            const steps = await response.json();
            return steps.map(s => ({
                tool: s.toolName,
                status: s.status === 'running' ? 'running' : (s.status === 'error' ? 'error' : 'done'),
                arguments: s.argumentsJson ? safeParseJson(s.argumentsJson) : null,
            }));
        } catch (error) {
            console.warn('Erro ao carregar passos:', error);
            return [];
        }
    };

    // Atualiza a última mensagem do usuário com o ID recebido
    const updateLastUserMessageWithId = (messages, messageId) => {
        console.log("Updating last user message with messages", messages, "and messageId", messageId);
        let lastUserMessageIdx = null;
        for (let i = messages.length - 1; i >= 0; i--) {
            if (messages[i].role === 'user') {
                lastUserMessageIdx = i;
                break;
            }
        }
        if (lastUserMessageIdx === null) return messages;

        const messagesBefore = messages.slice(0, lastUserMessageIdx);
        const messagesAfter = messages.slice(lastUserMessageIdx + 1);
        console.log("messages before = ", messagesBefore)
        console.log("messages after = ", messagesAfter)

        const response = messagesBefore.concat({
            ...messages[lastUserMessageIdx],
            id: messageId
        }).concat(messagesAfter);
        console.log("updated messages = ", response)
        return response;
    };

    // Verifica se é uma mensagem de sistema (boas-vindas ou erro)
    const isSystemMessage = (text) => {
        return text.includes('Welcome to DocIntel') ||
            text.includes('Sorry, I am unable to process') ||
            text.includes('I ran into some problems');
    };

    // Adiciona mensagem de sistema
    const handleSystemMessage = (messageText) => {
        setIsTyping(false);
        setLiveStep(null);
        setMessages(prev => [...prev, { role: 'assistant', text: messageText }]);
        currentStreamRef.current = null;
    };

    // Inicia o streaming de uma nova resposta
    const startStreaming = (messageText) => {
        setIsTyping(false);
        setLiveStep(null);
        const cleanedPending = sanitizeSteps(pendingStepsRef.current);
        currentStreamRef.current = { role: 'assistant', text: messageText, steps: cleanedPending };
        setMessages(prev => {
            const sanitized = sanitizeLastAssistantSteps(prev);
            const last = sanitized[sanitized.length - 1];
            if (last?.role === 'assistant' && last?.text === '' && Array.isArray(last?.steps)) {
                const merged = { ...currentStreamRef.current, steps: sanitizeSteps(last.steps) };
                currentStreamRef.current = merged;
                return sanitized.slice(0, -1).concat(merged);
            }
            return [...sanitized, currentStreamRef.current];
        });
        pendingStepsRef.current = [];
    };

    // Continua o streaming adicionando mais texto
    const continueStreaming = (messageText) => {
        setIsTyping(false);
        if (!currentStreamRef.current) {
            startStreaming(messageText);
            return;
        }
        const existingSteps = currentStreamRef.current.steps ?? [];
        const updatedText = currentStreamRef.current.text + messageText;
        currentStreamRef.current.text = updatedText;
        setMessages(prev => {
            const last = prev[prev.length - 1];
            const merged = {
                role: 'assistant',
                text: updatedText,
                steps: existingSteps.length ? existingSteps : (last?.steps ?? [])
            };
            currentStreamRef.current = merged;
            return prev.slice(0, -1).concat(merged);
        });
    };

    // Handler principal das mensagens do WebSocket
    const handleWebSocketMessage = (event) => {
        const messageText = event.data;

        console.log("Received WebSocket message:", messageText);

        // Tenta fazer parse como JSON (mensagem completa)
        try {
            const parsedMessage = JSON.parse(messageText);
            console.log("Parsed WebSocket JSON message:", parsedMessage);
            if (parsedMessage.type === 'tool_call') {
                const event = parsedMessage;
                if (event.status === 'start') {
                    setLiveStep({
                        tool: event.tool,
                        arguments: event.arguments ?? null,
                        status: 'running',
                    });
                } else if (event.status === 'end') {
                    setLiveStep(null);
                }
                setMessages(prev => {
                    const last = prev[prev.length - 1];
                    if (last?.role === 'assistant') {
                        const nextSteps = updateSteps(last.steps ?? [], event);
                        return prev.slice(0, -1).concat({ ...last, steps: nextSteps });
                    }

                    const nextSteps = updateSteps(pendingStepsRef.current, event);
                    pendingStepsRef.current = nextSteps;
                    return prev;
                });
                return;
            }

            if (!parsedMessage.messageId || !parsedMessage.content) {
                throw new Error("Invalid message format");
            }

            setMessages(prev => sanitizeLastAssistantSteps(updateLastUserMessageWithId(prev, parsedMessage.messageId)));
            setIsTyping(false);
            setLiveStep(null);
            pendingStepsRef.current = sanitizeSteps(pendingStepsRef.current);
            currentStreamRef.current = null;
            return;
        } catch (e) {
            // Não é JSON, continua com o processamento de streaming
        }

        // Mensagens de sistema (boas-vindas ou erros)
        if (isSystemMessage(messageText)) {
            handleSystemMessage(messageText);
            return;
        }

        // Streaming de resposta (chunks de texto)
        if (!currentStreamRef.current) {
            startStreaming(messageText);
        } else {
            continueStreaming(messageText);
        }
    };

    // Conecta ao WebSocket quando há um chatId
    useEffect(() => {
        if (!chatId || !wsUrl) {
            setMessages([]);
            return;
        }

        let shouldStop = false;

        const connect = () => {
            wsRef.current = new WebSocket(wsUrl);

            wsRef.current.onopen = () => {
                reconnectDelayRef.current = 1000;
                setIsConnected(true);
            };

            wsRef.current.onmessage = handleWebSocketMessage;

            wsRef.current.onclose = () => {
                setIsConnected(false);
                setLiveStep(null);
                currentStreamRef.current = null;
                pendingStepsRef.current = [];

                if (!shouldStop) {
                    setTimeout(() => {
                        reconnectDelayRef.current = Math.min(30000, reconnectDelayRef.current * 2);
                        connect();
                    }, reconnectDelayRef.current);
                }
            };

            wsRef.current.onerror = () => {
                setIsConnected(false);
                setLiveStep(null);
                currentStreamRef.current = null;
                pendingStepsRef.current = [];
                wsRef.current?.close();
            };
        };

        connect();
        loadPastMessages(chatId);

        return () => {
            shouldStop = true;
            currentStreamRef.current = null;
            pendingStepsRef.current = [];
            wsRef.current?.close();
        };
    }, [chatId, wsUrl]);

    // Envia mensagem pelo WebSocket
    const sendMessage = (text) => {
        console.log("sendMessage called with text =", text);
        if (!text?.trim() || !isConnected) return false;

        setMessages(prev => [...prev, { role: 'user', text: text.trim() }]);
        setIsTyping(true);
        setLiveStep(null);
        currentStreamRef.current = null;
        pendingStepsRef.current = [];

        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(text.trim());
            return true;
        } else {
            setIsTyping(false);
            setMessages(prev => [...prev, {
                role: 'assistant',
                text: 'Erro: Conexão não está aberta. Tente novamente.'
            }]);
            return false;
        }

    };

    return {
        messages,
        isConnected,
        isTyping,
        liveStep,
        sendMessage,
        fetchSteps
    };
};
