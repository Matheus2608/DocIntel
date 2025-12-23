import { useState, useEffect, useRef } from 'react';

/**
 * Hook customizado para gerenciar a conexão WebSocket e mensagens do chat
 * @param {string} chatId - ID do chat atual
 * @param {string} wsUrl - URL do WebSocket
 * @returns {object} Estado e funções do chat
 */
export const useWebSocketChat = (chatId, wsUrl) => {
    const [messages, setMessages] = useState([]);
    const [isConnected, setIsConnected] = useState(false);
    const [isTyping, setIsTyping] = useState(false);
    
    const wsRef = useRef(null);
    const currentStreamRef = useRef(null);

    // Conecta ao WebSocket quando há um chatId
    useEffect(() => {
        if (!chatId) {
            console.log('useWebSocketChat: Sem chatId, não conectando');
            return;
        }

        let shouldStop = false;
        let reconnectDelay = 1000;

        function connect() {
            console.info('useWebSocketChat: Conectando ao WebSocket...', wsUrl);
            wsRef.current = new WebSocket(wsUrl);

            wsRef.current.onopen = () => {
                reconnectDelay = 1000;
                setIsConnected(true);
                console.info('useWebSocketChat: WebSocket conectado!');
            };

            wsRef.current.onmessage = (ev) => {
                const messageText = ev.data;
                console.log('useWebSocketChat: Mensagem recebida:', messageText);

                // Mensagem de boas-vindas
                if (messageText.includes('Welcome to DocIntel')) {
                    setMessages((m) => [...m, { role: 'assistant', text: messageText }]);
                    return;
                }

                // Mensagens de erro
                if (messageText.includes('Sorry, I am unable to process') ||
                    messageText.includes('I ran into some problems')) {
                    setIsTyping(false);
                    setMessages((m) => [...m, { role: 'assistant', text: messageText }]);
                    currentStreamRef.current = null;
                    return;
                }

                // Streaming: acumula chunks
                setIsTyping(false);

                if (!currentStreamRef.current) {
                    // Primeiro chunk - cria nova mensagem
                    currentStreamRef.current = { role: 'assistant', text: messageText };
                    setMessages((m) => [...m, currentStreamRef.current]);
                } else {
                    // Chunks subsequentes - atualiza mensagem
                    currentStreamRef.current.text += messageText;
                    setMessages((m) => [...m.slice(0, -1), { ...currentStreamRef.current }]);
                }
            };

            wsRef.current.onclose = () => {
                setIsConnected(false);
                currentStreamRef.current = null;
                if (shouldStop) return;
                
                console.info('useWebSocketChat: Reconectando em', reconnectDelay, 'ms...');
                setTimeout(() => {
                    reconnectDelay = Math.min(30000, reconnectDelay * 2);
                    connect();
                }, reconnectDelay);
            };

            wsRef.current.onerror = (error) => {
                console.error('useWebSocketChat: Erro no WebSocket:', error);
                setIsConnected(false);
                currentStreamRef.current = null;
                try { wsRef.current?.close(); } catch (e) { }
            };
        }

        connect();

        return () => {
            shouldStop = true;
            currentStreamRef.current = null;
            try { wsRef.current?.close(); } catch (e) { }
        };
    }, [chatId, wsUrl]);

    // Função para enviar mensagem
    const sendMessage = (text) => {
        if (!text?.trim()) return;

        if (!isConnected) {
            console.warn('useWebSocketChat: WebSocket não conectado');
            return false;
        }

        // Adiciona mensagem do usuário
        setMessages((m) => [...m, { role: 'user', text: text.trim() }]);
        setIsTyping(true);
        currentStreamRef.current = null;

        // Envia para o WebSocket
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(text.trim());
            return true;
        } else {
            setIsTyping(false);
            setMessages((m) => [...m, {
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
        clearMessages: () => setMessages([])
    };
};
