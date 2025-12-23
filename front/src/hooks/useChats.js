import { useState, useEffect } from 'react';

/**
 * Hook para gerenciar a lista de chats do backend
 */
export const useChats = () => {
    const [chats, setChats] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    const backendApiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

    // Carrega lista de chats do backend
    const fetchChats = async () => {
        setIsLoading(true);
        setError(null);
        
        try {
            const response = await fetch(`${backendApiUrl}/api/chats`);
            
            if (!response.ok) {
                throw new Error('Erro ao carregar chats');
            }

            const data = await response.json();
            
            // Para cada chat, busca informações do documento
            const chatsWithDocInfo = await Promise.all(
                data.map(async (chat) => {
                    try {
                        // Busca informações do documento associado ao chat
                        const docResponse = await fetch(`${backendApiUrl}/api/chats/${chat.id}/document`);
                        if (docResponse.ok) {
                            const docInfo = await docResponse.json();
                            return {
                                ...chat,
                                fileName: docInfo.fileName,
                                fileType: docInfo.fileType
                            };
                        }
                    } catch (err) {
                        console.warn(`Erro ao buscar documento do chat ${chat.id}:`, err);
                    }
                    return chat;
                })
            );
            
            setChats(chatsWithDocInfo);
        } catch (err) {
            console.error('Erro ao carregar chats:', err);
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    // Adiciona um novo chat à lista
    const addChat = (chatData) => {
        setChats(prevChats => [chatData, ...prevChats]);
    };

    // Deleta um chat
    const deleteChat = async (chatId) => {
        try {
            const response = await fetch(`${backendApiUrl}/api/chats/${chatId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Erro ao deletar chat');
            }

            setChats(prevChats => prevChats.filter(chat => chat.id !== chatId));
            return true;
        } catch (err) {
            console.error('Erro ao deletar chat:', err);
            setError(err.message);
            return false;
        }
    };

    // Carrega chats ao montar
    useEffect(() => {
        fetchChats();
    }, []);

    return {
        chats,
        isLoading,
        error,
        fetchChats,
        addChat,
        deleteChat
    };
};
