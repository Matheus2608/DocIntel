import { useState, useEffect, useRef } from 'react';
import { API_BASE_URL } from '../config';

/**
 * Hook para gerenciar WebSocket e mensagens do chat
 */
export const useWebSocketChat = (chatId, wsUrl) => {
    const [messages, setMessages] = useState([]);
    const [isConnected, setIsConnected] = useState(false);
    const [isTyping, setIsTyping] = useState(false);

    const wsRef = useRef(null);
    const currentStreamRef = useRef(null);
    const reconnectDelayRef = useRef(1000);
    const tokenBufferRef = useRef([]);
    const displayIntervalRef = useRef(null);

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
                }));
                setMessages(formattedMessages);
            }
        } catch (error) {
            console.warn('Erro ao carregar mensagens:', error);
        }
    };

    // Atualiza a última mensagem do usuário com o ID recebido
    const updateLastUserMessageWithId = (messages, messageId) => {
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

        return messagesBefore.concat({
            ...messages[lastUserMessageIdx],
            id: messageId
        }).concat(messagesAfter);
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
        setMessages(prev => [...prev, { role: 'assistant', text: messageText }]);
        currentStreamRef.current = null;
    };

    // Starts the display interval that drains the token buffer at a fixed rate
    const startDisplayTimer = () => {
        if (displayIntervalRef.current !== null) return;

        displayIntervalRef.current = setInterval(() => {
            if (tokenBufferRef.current.length === 0) {
                clearInterval(displayIntervalRef.current);
                displayIntervalRef.current = null;
                return;
            }

            // Take up to 3 characters from the front of the buffer
            const chars = tokenBufferRef.current.splice(0, 3).join('');

            setMessages(prev => {
                const last = prev[prev.length - 1];
                if (last && last.role === 'assistant') {
                    const updatedText = last.text + chars;
                    currentStreamRef.current = { role: 'assistant', text: updatedText };
                    return prev.slice(0, -1).concat({ role: 'assistant', text: updatedText });
                }
                currentStreamRef.current = { role: 'assistant', text: chars };
                return [...prev, { role: 'assistant', text: chars }];
            });
        }, 30);
    };

    // Inicia o streaming de uma nova resposta
    const startStreaming = (messageText) => {
        setIsTyping(false);
        // Create the assistant message placeholder
        currentStreamRef.current = { role: 'assistant', text: '' };
        setMessages(prev => [...prev, currentStreamRef.current]);
        // Push all characters of the token into the buffer
        for (const ch of messageText) {
            tokenBufferRef.current.push(ch);
        }
        startDisplayTimer();
    };

    // Continua o streaming adicionando mais texto
    const continueStreaming = (messageText) => {
        setIsTyping(false);
        // Push all characters into the buffer
        for (const ch of messageText) {
            tokenBufferRef.current.push(ch);
        }
        startDisplayTimer();
    };

    // Handler principal das mensagens do WebSocket
    const handleWebSocketMessage = (event) => {
        const messageText = event.data;

        // Tenta fazer parse como JSON (mensagem completa)
        try {
            const parsedMessage = JSON.parse(messageText);
            if (!parsedMessage.messageId || !parsedMessage.content) {
                throw new Error("Invalid message format");
            }

            // Flush remaining buffer synchronously before finalising
            if (tokenBufferRef.current.length > 0) {
                const remaining = tokenBufferRef.current.splice(0).join('');
                setMessages(prev => {
                    const last = prev[prev.length - 1];
                    if (last && last.role === 'assistant') {
                        return prev.slice(0, -1).concat({ role: 'assistant', text: last.text + remaining });
                    }
                    return [...prev, { role: 'assistant', text: remaining }];
                });
            }
            // Stop display timer
            if (displayIntervalRef.current !== null) {
                clearInterval(displayIntervalRef.current);
                displayIntervalRef.current = null;
            }

            setMessages(prev => updateLastUserMessageWithId(prev, parsedMessage.messageId));
            setIsTyping(false);
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
                currentStreamRef.current = null;

                if (!shouldStop) {
                    setTimeout(() => {
                        reconnectDelayRef.current = Math.min(30000, reconnectDelayRef.current * 2);
                        connect();
                    }, reconnectDelayRef.current);
                }
            };

            wsRef.current.onerror = () => {
                setIsConnected(false);
                currentStreamRef.current = null;
                wsRef.current?.close();
            };
        };

        connect();
        loadPastMessages(chatId);

        return () => {
            shouldStop = true;
            currentStreamRef.current = null;
            tokenBufferRef.current = [];
            if (displayIntervalRef.current !== null) {
                clearInterval(displayIntervalRef.current);
                displayIntervalRef.current = null;
            }
            wsRef.current?.close();
        };
    }, [chatId, wsUrl]);

    // Envia mensagem pelo WebSocket
    const sendMessage = (text) => {
        if (!text?.trim() || !isConnected) return false;

        setMessages(prev => [...prev, { role: 'user', text: text.trim() }]);
        setIsTyping(true);
        currentStreamRef.current = null;

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
        sendMessage,
        tokenBufferRef
    };
};