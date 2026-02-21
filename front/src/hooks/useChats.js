import { useState, useEffect } from 'react';
import { API_BASE_URL } from '../config';

/**
 * Hook para gerenciar a lista de chats do backend
 */
export const useChats = () => {
    const [chats, setChats] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    // Carrega lista de chats do backend
    const fetchChats = async () => {
        setIsLoading(true);
        setError(null);

        try {
            const response = await fetch(API_BASE_URL);

            if (!response.ok) {
                throw new Error('Erro ao carregar chats');
            }

            const data = await response.json();
            setChats(data);
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
    const deleteChat = (chatId) => {
        fetch(`http://localhost:8080/api/chats/${chatId}`,
            { method: 'DELETE' }
        ).then(response => {
            if (response.ok) {
                console.log('Chat deleted successfully');
                setChats(prevChats => prevChats.filter(chat => chat.id !== chatId));
            } else {
                console.error('Failed to delete chat messages');
            }
        }).catch(error => {
            console.error('Error deleting chat messages:', error);
        });
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
