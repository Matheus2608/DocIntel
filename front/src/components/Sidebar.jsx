import {
    Plus,
    FileText,
    Settings,
    LogOut,
    Trash2,
    Moon,
    Sun,
    Loader2,
    Database
} from 'lucide-react';
import { getDarkModeClasses } from '../config';

export const Sidebar = ({ 
    chats, 
    activeChatId, 
    isDarkMode, 
    setIsDarkMode, 
    onNewChat, 
    onSelectChat, 
    onDeleteChat,
    onNavigateToEmbedding,
    currentView,
    isLoading 
}) => {
    const { darkModeBg, pageBgAndText, borderColor } = getDarkModeClasses(isDarkMode);

    const handleDeleteClick = (e, chatId) => {
        e.stopPropagation(); // Evita selecionar o chat ao clicar em deletar
        if (window.confirm('Tem certeza que deseja deletar este chat?')) {
            onDeleteChat(chatId);
        }
    };

    return (
        <aside className={`w-1/4 flex flex-col text-white transition-colors ${isDarkMode ? 'bg-[#202123]' : 'bg-[#202123]'}`}>
            {/* Botão Novo Chat */}
            <div className="p-4">
                <button 
                    className="w-full flex items-center gap-3 px-4 py-3 border border-gray-600 rounded-md hover:bg-gray-700 transition-colors text-sm" 
                    onClick={() => setIsDarkMode(!isDarkMode)}
                >
                    {isDarkMode ? <Sun size={16} /> : <Moon size={16} />}
                    {isDarkMode ? 'Light Mode' : 'Dark Mode'}
                </button>
                <button 
                    className="w-full flex items-center gap-3 px-4 py-3 border border-gray-600 rounded-md hover:bg-gray-700 transition-colors text-sm mt-2"
                    onClick={onNewChat}
                >
                    <Plus size={16} />
                    New Chat
                </button>
            </div>

            {/* Lista de Conversas (Scrollable) */}
            <nav className="flex-1 overflow-y-auto px-2 space-y-1 custom-scrollbar">
                <p className="text-xs text-gray-500 font-semibold px-3 py-2 uppercase">Recent</p>
                
                {isLoading && (
                    <div className="flex items-center justify-center py-4">
                        <Loader2 size={20} className="animate-spin text-gray-400" />
                    </div>
                )}

                {!isLoading && chats.length === 0 && (
                    <p className="text-xs text-gray-500 px-3 py-2 text-center">
                        Nenhum chat ainda. Crie um novo!
                    </p>
                )}

                {!isLoading && chats.map((chat) => (
                    <div
                        key={chat.id}
                        className={`group flex items-center gap-3 px-3 py-3 rounded-md cursor-pointer transition-colors ${
                            chat.id === activeChatId ? 'bg-gray-700' : 'hover:bg-gray-800'
                        }`}
                        onClick={() => onSelectChat(chat.id)}
                    >
                        <FileText size={16} className="text-gray-400" />
                        <span className="flex-1 text-sm truncate">
                            {chat.fileName || chat.title || `Chat ${chat.id.substring(0, 8)}`}
                        </span>

                        {/* Ícone de deletar (aparece no hover) */}
                        {chat.id === activeChatId && (
                            <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <Trash2 
                                    size={14} 
                                    className="hover:text-red-400"
                                    onClick={(e) => handleDeleteClick(e, chat.id)}
                                />
                            </div>
                        )}
                    </div>
                ))}
            </nav>

            {/* Footer da Sidebar */}
            <div className="p-4 border-t border-gray-700 space-y-1">
                <button 
                    className={`w-full flex items-center gap-3 px-3 py-3 hover:bg-gray-800 rounded-md text-sm transition-colors ${
                        currentView === 'embedding' ? 'bg-gray-700' : ''
                    }`}
                    onClick={onNavigateToEmbedding}
                >
                    <Database size={16} />
                    Embedding Search
                </button>
                <div className="flex items-center gap-3 px-3 py-3 hover:bg-gray-800 rounded-md text-sm cursor-pointer border-t border-gray-800">
                    <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-xs font-bold">
                        A
                    </div>
                    <span className="flex-1 truncate">Admin</span>
                    <LogOut size={14} className="text-gray-500" />
                </div>
            </div>
        </aside>
    )
}