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
                    role: msg.role,
                    text: msg.content
                }));
                setMessages(formattedMessages);
            }
        } catch (error) {
            console.warn('Erro ao carregar mensagens:', error);
        }
    };

    // Conecta ao WebSocket quando há um chatId
    useEffect(() => {
        if (!chatId || !wsUrl) return;

        let shouldStop = false;

        const connect = () => {
            wsRef.current = new WebSocket(wsUrl);

            wsRef.current.onopen = () => {
                reconnectDelayRef.current = 1000;
                setIsConnected(true);
            };

            wsRef.current.onmessage = (event) => {
                const messageText = event.data;

                // Mensagem de boas-vindas ou erro
                if (messageText.includes('Welcome to DocIntel') ||
                    messageText.includes('Sorry, I am unable to process') ||
                    messageText.includes('I ran into some problems')) {
                    setIsTyping(false);
                    setMessages(prev => [...prev, { role: 'assistant', text: messageText }]);
                    currentStreamRef.current = null;
                    return;
                }

                // Streaming de resposta
                setIsTyping(false);
                if (!currentStreamRef.current) {
                    currentStreamRef.current = { role: 'assistant', text: messageText };
                    setMessages(prev => [...prev, currentStreamRef.current]);
                } else {
                    currentStreamRef.current.text += messageText;
                    setMessages(prev => [...prev.slice(0, -1), { ...currentStreamRef.current }]);
                }
            };

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
        if (!text?.trim() || !isConnected) return false;

        setMessages(prev => [...prev, { role: 'user', text: text.trim() }]);
        setIsTyping(true);
        currentStreamRef.current = null;

        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(text.trim());
            return true;
        }

        setIsTyping(false);
        setMessages(prev => [...prev, {
            role: 'assistant',
            text: 'Erro: Conexão não está aberta. Tente novamente.'
        }]);
        return false;
    };

    return {
        messages,
        isConnected,
        isTyping,
        sendMessage,
        clearMessages: () => setMessages([])
    };
};
