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
        setMessages(prev => [...prev, { role: 'assistant', text: messageText }]);
        currentStreamRef.current = null;
    };

    // Inicia o streaming de uma nova resposta
    const startStreaming = (messageText) => {
        setIsTyping(false);
        currentStreamRef.current = { role: 'assistant', text: messageText };
        setMessages(prev => [...prev, currentStreamRef.current]);
    };

    // Continua o streaming adicionando mais texto
    const continueStreaming = (messageText) => {
        setIsTyping(false);
        const updatedText = currentStreamRef.current.text + messageText;
        currentStreamRef.current.text = updatedText;
        setMessages(prev => {
            const newMessage = prev.slice(0, -1).concat({role: 'assistant', text: updatedText});
            console.log("Updated streaming message:", newMessage);
            return newMessage;
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
            if (!parsedMessage.messageId || !parsedMessage.content) {
                console.log("Invalid message format, missing fields");
                throw new Error("Invalid message format");
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
            wsRef.current?.close();
        };
    }, [chatId, wsUrl]);

    // Envia mensagem pelo WebSocket
    const sendMessage = (text) => {
        console.log("sendMessage called with text =", text);
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
        sendMessage
    };
};