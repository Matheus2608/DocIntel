/**
 * Configurações centralizadas do aplicativo
 */

// URL base da API REST
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/chats';

// URL base da API de Embeddings
export const EMBEDDINGS_API_URL = import.meta.env.VITE_EMBEDDINGS_API_URL || 'http://localhost:8080/api/embeddings';

// URL base do WebSocket
export const WS_BASE_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080';

/**
 * Constrói a URL do WebSocket para um chat específico
 */
export const getWebSocketUrl = (chatId) => {
  if (!chatId) return null;
  return `${WS_BASE_URL}/document-support-agent/${chatId}`;
};

/**
 * Retorna classes CSS para o modo escuro
 */
export const getDarkModeClasses = (isDarkMode) => ({
  darkModeBg: isDarkMode ? 'bg-[#444654]' : 'bg-white',
  pageBgAndText: isDarkMode ? 'bg-[#343541] text-gray-100' : 'bg-gray-50 text-gray-800',
  borderColor: isDarkMode ? 'border-gray-600' : 'border-gray-300'
});
