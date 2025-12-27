import { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { Main } from './components/Main';
import { getDarkModeClasses } from './config';
import { useChats } from './hooks/useChats';

const App = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [activeChatId, setActiveChatId] = useState(null);
  
  // Hook para gerenciar chats do backend
  const { chats, isLoading, addChat, deleteChat } = useChats();

  const { pageBgAndText } = getDarkModeClasses(isDarkMode);

  // Encontra o chat ativo
  const activeChat = chats.find(chat => chat.id === activeChatId);

  // Handler para criar novo chat
  const handleNewChat = () => {
    setActiveChatId(null); // Volta para tela de upload
  };

  // Handler quando upload é bem-sucedido
  const handleChatCreated = (chatData) => {
    addChat(chatData);
    setActiveChatId(chatData.id);
  };

  // Handler para selecionar chat
  const handleSelectChat = (chatId) => {
    setActiveChatId(chatId);
  };

  // Handler para deletar chat
  const handleDeleteChat = async (chatId) => {
    const success = await deleteChat(chatId);
    if (success && chatId === activeChatId) {
      setActiveChatId(null);
    }
  };

  return (
    <div className={`flex h-screen transition-colors ${pageBgAndText}`}>
      <Sidebar 
        chats={chats} 
        activeChatId={activeChatId}
        isDarkMode={isDarkMode} 
        setIsDarkMode={setIsDarkMode}
        onNewChat={handleNewChat}
        onSelectChat={handleSelectChat}
        onDeleteChat={handleDeleteChat}
        isLoading={isLoading}
      />
      <Main 
        isDarkMode={isDarkMode}
        currentChat={activeChat}
        onChatCreated={handleChatCreated}
      />
    </div>
  );
};

export default App;