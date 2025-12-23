import { useState } from 'react';
import { Send } from 'lucide-react';
import { darkModeBackground } from '../shared/constants';

export const InputMessage = ({ isDarkMode, isConnected, onSendMessage }) => {
    const { darkModeBg, borderColor } = darkModeBackground(isDarkMode);
    const [input, setInput] = useState('');

    const handleSend = () => {
        const text = input.trim();
        if (!text) return;

        if (!isConnected) {
            alert('Aguarde a conexão com o servidor...');
            return;
        }

        // Chama callback do componente pai
        if (onSendMessage && onSendMessage(text)) {
            setInput(''); // Limpa input apenas se enviou com sucesso
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className={`p-6 border-t transition-colors border-none`}>
            <div className={`flex gap-3 border rounded-xl p-2 shadow-sm focus-within:ring-2 focus-within:ring-blue-500 transition-all ${darkModeBg} ${borderColor}`}>
                <input
                    type="text"
                    placeholder={isConnected ? "Digite sua pergunta aqui..." : "Upload a document first"}
                    className={`flex-1 px-4 py-2 outline-none transition-colors ${darkModeBg} ${isDarkMode ? 'text-gray-white placeholder-white' : 'text-gray-600 placeholder-gray-400'}`}
                    disabled={!isConnected}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                />
                <button 
                    className="bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 transition-colors"
                    disabled={!isConnected || !input.trim()}
                    onClick={handleSend}
                >
                    <Send size={18} />
                </button>
            </div>
        </div>
    );
};