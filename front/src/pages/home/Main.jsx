import {
  MessageSquare,
  UploadCloud,
  Send
} from 'lucide-react';
import { darkModeBackground } from '../../shared/constants';

export const Main = ({ isDarkMode }) => {
    const { darkModeBg, borderColor } = darkModeBackground(isDarkMode);

    return (
        <main className="flex-1 flex flex-col w-full h-full items-center justify-center">
            <div className={`w-full max-w-3xl rounded-md shadow-sm border-none overflow-hidden flex flex-col h-full transition-colors`}>

                {/* Header do Chat */}
                {/* Mantém azul/branco mesmo no modo escuro */}
                <div className="bg-blue-600 p-6 border-none rounded-2xl m-4 text-white shrink-0 items-center justify-center flex flex-col">
                    <h2 className="text-xl font-bold flex items-center gap-2">
                        <MessageSquare size={20} />
                        Document Q&A
                    </h2>
                    <p className={`text-sm`}>Faça perguntas sobre seus PDFs ou documentos Word.</p>
                </div>

                {/* Área de Upload / Histórico de Mensagens */}
                <div className={`border-none flex-1 p-8 overflow-y-auto flex flex-col justify-center items-center border-none transition-colors`}>
                    {/* Dropzone que criamos antes */}
                    {/* Mantém branco/azul mesmo no modo escuro */}
                    <div className={`w-full bg-white border-2 border-dashed rounded-2xl p-16 flex flex-col items-center justify-center transition-all cursor-pointer group border-gray-300 hover:border-blue-400 hover:bg-blue-50`}>
                        <div className={`p-4 rounded-full mb-4 group-hover:scale-110 transition-transform bg-blue-100`}>
                            <UploadCloud size={32} className="text-blue-600" />
                        </div>
                        <p className={`text-lg font-medium text-gray-700`}>Click to upload PDF or DOC</p>
                        <p className={`text-sm mt-1 text-gray-400`}>ou arraste o arquivo aqui</p>
                    </div>
                </div>

                {/* Input de Pergunta (Fixo na parte inferior) */}
                <div className={`p-6 border-t transition-colors border-none`}>
                    <div className={`flex gap-3 border rounded-xl p-2 shadow-sm focus-within:ring-2 focus-within:ring-blue-500 transition-all ${darkModeBg} ${borderColor}`}>
                        <input
                            type="text"
                            placeholder="Upload a document first"
                            className={`flex-1 px-4 py-2 outline-none transition-colors ${darkModeBg} ${isDarkMode ? 'text-gray-white placeholder-white' : 'text-gray-600 placeholder-gray-400'}`}
                            disabled
                        />
                        <button className="bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 transition-colors">
                            <Send size={18} />
                        </button>
                    </div>
                </div>

            </div>
        </main>
    )
}