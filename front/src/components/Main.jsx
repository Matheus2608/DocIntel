import { ChatMessage } from './ChatMessage';
import { Header } from './Header';
import { InputMessage } from './InputMessage';
import { Upload } from './Upload';
import { useWebSocketChat } from '../hooks/useWebSocketChat';
import { getWebSocketUrl } from '../config';

export const Main = ({ isDarkMode, currentChat, onChatCreated }) => {
    const wsUrl = getWebSocketUrl(currentChat?.id);

    const { messages, isConnected, isTyping, sendMessage } = useWebSocketChat(
        currentChat?.id,
        wsUrl
    );

    // Callback quando o upload for bem-sucedido
    const handleUploadSuccess = (chatData) => {
        if (onChatCreated) {
            onChatCreated(chatData);
        }
    };

    // Determina o texto do header
    const getHeaderText = () => {
        if (!currentChat) {
            return "Faça perguntas sobre seus PDFs ou documentos Word.";
        }
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
                        isDarkMode={isDarkMode}
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