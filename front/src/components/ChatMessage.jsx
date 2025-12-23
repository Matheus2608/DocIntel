import { useEffect, useRef } from 'react';
import { Bot, User } from 'lucide-react';

/**
 * Componente para exibir a lista de mensagens do chat
 * Responsabilidade: Apenas renderização das mensagens e auto-scroll
 */
export const ChatMessage = ({ messages, isTyping }) => {
    const messagesEndRef = useRef(null);

    // Auto-scroll para a última mensagem
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
            {messages.length === 0 && (
                <div className="text-center text-gray-500 mt-8">
                    <Bot className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                    <p className="text-lg font-medium">Olá! Como posso ajudá-lo?</p>
                    <p className="text-sm">Faça uma pergunta sobre seu documento</p>
                </div>
            )}

            {messages.map((m, i) => (
                <div
                    key={i}
                    className={`flex gap-3 ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                    {m.role === 'assistant' && (
                        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                            <Bot className="w-5 h-5 text-white" />
                        </div>
                    )}

                    <div
                        className={`max-w-[75%] rounded-2xl px-4 py-3 ${m.role === 'user'
                            ? 'bg-blue-600 text-white rounded-tr-sm'
                            : 'bg-white text-gray-800 shadow-sm rounded-tl-sm border border-gray-200'
                            }`}
                    >
                        <p className="text-sm whitespace-pre-wrap break-words">{m.text}</p>
                    </div>

                    {m.role === 'user' && (
                        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center">
                            <User className="w-5 h-5 text-white" />
                        </div>
                    )}
                </div>
            ))}

            {isTyping && (
                <div className="flex gap-3 justify-start">
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                        <Bot className="w-5 h-5 text-white" />
                    </div>
                    <div className="bg-white rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm border border-gray-200">
                        <div className="flex gap-1">
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                        </div>
                    </div>
                </div>
            )}

            <div ref={messagesEndRef} />
        </div>
    );
};