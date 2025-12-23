import { useState, useEffect } from 'react';
import { ChatMessage } from '../../components/ChatMessage';
import { Header } from '../../components/Header';
import { InputMessage } from '../../components/InputMessage';
import { Upload } from '../../components/Upload';
import { useWebSocketChat } from '../../hooks/useWebSocketChat';

export const Main = ({ isDarkMode, currentChat, onChatCreated }) => {
    // Configurar URL do WebSocket
    const backendWsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080';
    const wsUrl = `${backendWsUrl}/document-support-agent`;

    // Hook customizado para gerenciar WebSocket e mensagens
    const { messages, isConnected, isTyping, sendMessage, clearMessages } = useWebSocketChat(
        currentChat?.id,
        wsUrl
    );

    // Limpa mensagens quando troca de chat
    useEffect(() => {
        clearMessages();
    }, [currentChat?.id]);

    // Callback quando o upload for bem-sucedido
    const handleUploadSuccess = (chatData) => {
        console.log('Upload bem-sucedido! Chat criado:', chatData);
        if (onChatCreated) {
            onChatCreated(chatData);
        }
    };

    // Determina o texto do header
    const getHeaderText = () => {
        if (!currentChat) {
            return "Faça perguntas sobre seus PDFs ou documentos Word.";
        }
        // Tenta pegar o nome do arquivo, senão usa o ID
        return `Chat: ${currentChat.fileName || currentChat.title || currentChat.id.substring(0, 8)}`;
    };

    return (
        <main className="flex-1 flex flex-col w-full h-full items-center justify-center">
            <div className={`w-full max-w-3xl rounded-md shadow-sm border-none overflow-hidden flex flex-col h-full transition-colors`}>
                <Header 
                    headerText={"Document Q&A"} 
                    paragraphText={getHeaderText()} 
                />
                
                {currentChat ? (
                    <ChatMessage 
                        messages={messages}
                        isTyping={isTyping}
                    />
                ) : (
                    <Upload onUploadSuccess={handleUploadSuccess} />
                )}
                
                <InputMessage 
                    isDarkMode={isDarkMode} 
                    isConnected={isConnected && currentChat !== null} 
                    onSendMessage={sendMessage}
                />
            </div>
        </main>
    );
};