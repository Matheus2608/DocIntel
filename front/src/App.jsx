import { useState } from 'react';
import { Sidebar } from './pages/home/Sidebar';
import { Main } from './pages/home/Main';
import { darkModeBackground } from './shared/constants';
import { useChats } from './hooks/useChats';

const App = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [activeChatId, setActiveChatId] = useState(null);
  
  // Hook para gerenciar chats do backend
  const { chats, isLoading, addChat, deleteChat } = useChats();

  const { pageBgAndText } = darkModeBackground(isDarkMode);

  // Encontra o chat ativo
  const activeChat = chats.find(chat => chat.id === activeChatId);

  // Handler para criar novo chat
  const handleNewChat = () => {
    setActiveChatId(null); // Volta para tela de upload
  };

  // Handler quando upload é bem-sucedido
  const handleChatCreated = (chatData) => {
    addChat(chatData); // Adiciona à lista
    setActiveChatId(chatData.id); // Ativa o novo chat
  };

  // Handler para selecionar chat
  const handleSelectChat = (chatId) => {
    setActiveChatId(chatId);
  };

  // Handler para deletar chat
  const handleDeleteChat = async (chatId) => {
    const success = await deleteChat(chatId);
    if (success && chatId === activeChatId) {
      setActiveChatId(null); // Se deletou o chat ativo, volta para upload
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