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
  Send
} from 'lucide-react';

const App = () => {
  // Estado para simular as conversas anteriores
  const [chats, setChats] = useState([
    { id: 1, title: 'Análise Contrato_A.pdf', active: true },
    { id: 2, title: 'Resumo Relatório_RH.pdf', active: false },
    { id: 3, title: 'Docs Fiscais 2024.doc', active: false },
  ]);

  return (
    <div className="flex h-screen bg-gray-50 text-gray-800">
      {/* SIDEBAR */}
      <aside className="w-1/4 bg-[#202123] flex flex-col text-white">
        {/* Botão Novo Chat */}
        <div className="p-4">
          <button className="w-full flex items-center gap-3 px-4 py-3 border border-gray-600 rounded-md hover:bg-gray-700 transition-colors text-sm">
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
        <div className="w-full max-w-3xl bg-white rounded-md shadow-sm border-none overflow-hidden flex flex-col h-full">
          
          {/* Header do Chat */}
          <div className="bg-blue-600 p-6 border rounded-2xl m-4 text-white shrink-0 items-center justify-center flex flex-col">
            <h2 className="text-xl font-bold flex items-center gap-2">
              <MessageSquare size={20} />
              Document Q&A
            </h2>
            <p className="text-blue-100 text-sm">Faça perguntas sobre seus PDFs ou documentos Word.</p>
          </div>

          {/* Área de Upload / Histórico de Mensagens */}
          <div className="flex-1 p-8 overflow-y-auto flex flex-col justify-center items-center border-none">
             {/* Dropzone que criamos antes */}
             <div className="w-full border-2 border-dashed border-gray-300 rounded-2xl p-16 flex flex-col items-center justify-center hover:border-blue-400 hover:bg-blue-50 transition-all cursor-pointer group">
                <div className="bg-blue-100 p-4 rounded-full mb-4 group-hover:scale-110 transition-transform">
                  <UploadCloud size={32} className="text-blue-600" />
                </div>
                <p className="text-lg font-medium text-gray-700">Click to upload PDF or DOC</p>
                <p className="text-sm text-gray-400 mt-1">ou arraste o arquivo aqui</p>
             </div>
          </div>

          {/* Input de Pergunta (Fixo na parte inferior) */}
          <div className="p-6 border-t border-gray-100 bg-gray-50">
            <div className="flex gap-3 bg-white border border-gray-300 rounded-xl p-2 shadow-sm focus-within:ring-2 focus-within:ring-blue-500 transition-all">
              <input 
                type="text" 
                placeholder="Upload a document first"
                className="flex-1 px-4 py-2 outline-none text-gray-600"
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