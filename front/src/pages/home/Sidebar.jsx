import {
    Plus,
    FileText,
    Settings,
    LogOut,
    Trash2,
    Edit3,
    Moon,
    Sun
} from 'lucide-react';

export const Sidebar = ({ chats, isDarkMode }) => {

    const darkModeBg = isDarkMode ? 'bg-[#444654]' : 'bg-white';
    const pageBgAndText = isDarkMode ? 'bg-[#343541] text-gray-100' : 'bg-gray-50 text-gray-800';
    const borderColor = isDarkMode ? 'border-gray-600' : 'border-gray-300';

    return (
        <aside className={`w-1/4 flex flex-col text-white transition-colors ${isDarkMode ? 'bg-[#202123]' : 'bg-[#202123]'}`}>
            {/* Botão Novo Chat */}
            <div className="p-4">
                <button className="w-full flex items-center gap-3 px-4 py-3 border border-gray-600 rounded-md hover:bg-gray-700 transition-colors text-sm" onClick={() => setIsDarkMode(!isDarkMode)}>
                    {isDarkMode ? <Sun size={16} /> : <Moon size={16} />}
                    {isDarkMode ? 'Light Mode' : 'Dark Mode'}
                </button>
                <button className="w-full flex items-center gap-3 px-4 py-3 border border-gray-600 rounded-md hover:bg-gray-700 transition-colors text-sm mt-2">
                    <Plus size={16} />
                    New Chat
                </button>
            </div>

            {/* Lista de Conversas (Scrollable) */}
            <nav className="flex-1 overflow-y-auto px-2 space-y-1 custom-scrollbar">
                <p className="text-xs text-gray-500 font-semibold px-3 py-2 uppercase">Recent</p>
                {chats.map((chat) => (
                    <div
                        key={chat.id}
                        className={`group flex items-center gap-3 px-3 py-3 rounded-md cursor-pointer transition-colors ${chat.active ? 'bg-gray-700' : 'hover:bg-gray-800'
                            }`}
                    >
                        <FileText size={16} className="text-gray-400" />
                        <span className="flex-1 text-sm truncate">{chat.title}</span>

                        {/* Ícones de ação (aparecem no hover) */}
                        {chat.active && (
                            <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                <Edit3 size={14} className="hover:text-blue-400" />
                                <Trash2 size={14} className="hover:text-red-400" />
                            </div>
                        )}
                    </div>
                ))}
            </nav>

            {/* Footer da Sidebar */}
            <div className="p-4 border-t border-gray-700 space-y-1">
                <button className="w-full flex items-center gap-3 px-3 py-3 hover:bg-gray-800 rounded-md text-sm transition-colors">
                    <Settings size={16} />
                    Settings
                </button>
                <div className="flex items-center gap-3 px-3 py-3 hover:bg-gray-800 rounded-md text-sm cursor-pointer border-t border-gray-800">
                    <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-xs font-bold">
                        U
                    </div>
                    <span className="flex-1 truncate">Usuário Logado</span>
                    <LogOut size={14} className="text-gray-500" />
                </div>
            </div>
        </aside>
    )
}