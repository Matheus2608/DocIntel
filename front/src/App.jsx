import React, { useState } from 'react';
import { 
  Plus, 
  MessageSquare, 
  FileText, 
  Settings, 
  LogOut, 
  Trash2, 
  Edit3,
  UploadCloud,
  Send,
  Moon,
  Sun
} from 'lucide-react';

const App = () => {
    // Estado para o modo escuro
    const [isDarkMode, setIsDarkMode] = useState(false);

  // Classe de background principal para componentes escuros na main
  const darkModeBg = isDarkMode ? 'bg-[#444654]' : 'bg-white';
  const pageBgAndText = isDarkMode ? 'bg-[#343541] text-gray-100' : 'bg-gray-50 text-gray-800';
  const borderColor = isDarkMode ? 'border-gray-600' : 'border-gray-300';

  // Estado para simular as conversas anteriores
  const [chats, setChats] = useState([
    { id: 1, title: 'Análise Contrato_A.pdf', active: true },
    { id: 2, title: 'Resumo Relatório_RH.pdf', active: false },
    { id: 3, title: 'Docs Fiscais 2024.doc', active: false },
  ]);

  return (
    <div className={`flex h-screen transition-colors ${pageBgAndText}`}>
      {/* SIDEBAR */}
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
              className={`group flex items-center gap-3 px-3 py-3 rounded-md cursor-pointer transition-colors ${
                chat.active ? 'bg-gray-700' : 'hover:bg-gray-800'
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

      {/* CONTEÚDO PRINCIPAL (Main Content) */}
      <main className="flex-1 flex flex-col w-full h-full items-center justify-center">
        <div className={`w-full max-w-3xl rounded-md shadow-sm border-none overflow-hidden flex flex-col h-full transition-colors ${darkModeBg}`}>
          
          {/* Header do Chat */}
          {/* Mantém azul/branco mesmo no modo escuro */}
          <div className="bg-blue-600 p-6 border rounded-2xl m-4 text-white shrink-0 items-center justify-center flex flex-col">
            <h2 className="text-xl font-bold flex items-center gap-2">
              <MessageSquare size={20} />
              Document Q&A
            </h2>
            <p className={`text-sm ${isDarkMode ? 'text-gray-300' : 'text-blue-100'}`}>Faça perguntas sobre seus PDFs ou documentos Word.</p>
          </div>

          {/* Área de Upload / Histórico de Mensagens */}
          <div className={`flex-1 p-8 overflow-y-auto flex flex-col justify-center items-center border-none transition-colors`}>
             {/* Dropzone que criamos antes */}
             {/* Mantém branco/azul mesmo no modo escuro */}
             <div className={`w-full border-2 border-dashed rounded-2xl p-16 flex flex-col items-center justify-center transition-all cursor-pointer group border-gray-300 hover:border-blue-400 hover:bg-blue-50`}>
               <div className={`p-4 rounded-full mb-4 group-hover:scale-110 transition-transform bg-blue-100`}>
                  <UploadCloud size={32} className="text-blue-600" />
                </div>
                <p className={`text-lg font-medium text-gray-700`}>Click to upload PDF or DOC</p>
                <p className={`text-sm mt-1 text-gray-400`}>ou arraste o arquivo aqui</p>
             </div>
          </div>

          {/* Input de Pergunta (Fixo na parte inferior) */}
          <div className={`p-6 border-t transition-colors ${darkModeBg} ${isDarkMode ? 'border-gray-700' : 'border-gray-100'}`}>
            <div className={`flex gap-3 border rounded-xl p-2 shadow-sm focus-within:ring-2 focus-within:ring-blue-500 transition-all ${darkModeBg} ${borderColor}`}>
              <input 
                type="text" 
                placeholder="Upload a document first"
                className={`flex-1 px-4 py-2 outline-none transition-colors ${darkModeBg} ${isDarkMode ? 'text-gray-200 placeholder-gray-500' : 'text-gray-600 placeholder-gray-400'}`}
                disabled
              />
              <button className="bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 transition-colors">
                <Send size={18} />
              </button>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
};

export default App;