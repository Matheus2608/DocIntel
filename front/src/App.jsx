import { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { Main } from './components/Main';
import EmbeddingSearch from './components/EmbeddingSearch';
import { getDarkModeClasses } from './config';
import { useChats } from './hooks/useChats';
import RagModal from './components/RagModal';

const App = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [activeChatId, setActiveChatId] = useState(null);
  const [currentView, setCurrentView] = useState('chat'); // 'chat' or 'embedding'

  // Hook para gerenciar chats do backend
  const { chats, isLoading, addChat, deleteChat } = useChats();

  const { pageBgAndText } = getDarkModeClasses(isDarkMode);

  // Encontra o chat ativo
  const activeChat = chats.find(chat => chat.id === activeChatId);

  // Handler para criar novo chat
  const handleNewChat = () => {
    setActiveChatId(null); // Volta para tela de upload
    setCurrentView('chat');
  };

  // Handler quando upload é bem-sucedido
  const handleChatCreated = (chatData) => {
    addChat(chatData);
    setActiveChatId(chatData.id);
    setCurrentView('chat');
  };

  // Handler para selecionar chat
  const handleSelectChat = (chatId) => {
    setActiveChatId(chatId);
    setCurrentView('chat');
  };

  // Handler para deletar chat
  const handleDeleteChat = async (chatId) => {
    const success = await deleteChat(chatId);
    if (success && chatId === activeChatId) {
      setActiveChatId(null);
    }
  };

  // Handler para navegar para embedding search
  const handleNavigateToEmbedding = () => {
    setCurrentView('embedding');
    setActiveChatId(null);
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
        onNavigateToEmbedding={handleNavigateToEmbedding}
        currentView={currentView}
        isLoading={isLoading}
      />
      {currentView === 'chat' ? (
        <Main
          isDarkMode={isDarkMode}
          currentChat={activeChat}
          onChatCreated={handleChatCreated}
        />
      ) : (
        <EmbeddingSearch />
      )}
    </div>
  );
};

export default App;