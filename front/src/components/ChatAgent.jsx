import { useState, useEffect, useRef } from 'react';
import { Send, Bot, User } from 'lucide-react';

export default function ChatAgent({ documentId }) {
  const [messages, setMessages] = useState([]); // { role: 'user'|'assistant', text: string }
  const [input, setInput] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const wsRef = useRef(null);
  const messagesEndRef = useRef(null);
  const currentStreamRef = useRef(null); // Para acumular mensagens em streaming

  // Configurar URL do WebSocket para o backend
  const backendWsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080';
  const wsUrl = `${backendWsUrl}/document-support-agent`;

  // Auto-scroll para a última mensagem
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    let shouldStop = false;
    let reconnectDelay = 1000;

    function connect() {
      console.info('ChatAgent: Tentando conectar ao WebSocket...', wsUrl);
      wsRef.current = new WebSocket(wsUrl);

      wsRef.current.onopen = () => {
        reconnectDelay = 1000;
        setIsConnected(true);
        console.info('ChatAgent: WebSocket conectado!');
      };

      wsRef.current.onmessage = (ev) => {
        const messageText = ev.data;
        console.log('ChatAgent: Mensagem recebida:', messageText);
        
        // Verifica se é a mensagem de boas-vindas (@OnOpen)
        if (messageText.includes('Welcome to DocIntel')) {
          setMessages((m) => [...m, { role: 'assistant', text: messageText }]);
          return;
        }

        // Verifica se é uma mensagem de erro
        if (messageText.includes('Sorry, I am unable to process') || 
            messageText.includes('I ran into some problems')) {
          setIsTyping(false);
          setMessages((m) => [...m, { role: 'assistant', text: messageText }]);
          currentStreamRef.current = null;
          return;
        }

        // Mensagens em streaming: acumula os chunks
        setIsTyping(false);
        
        if (!currentStreamRef.current) {
          // Primeira parte da resposta - cria nova mensagem
          currentStreamRef.current = { role: 'assistant', text: messageText };
          setMessages((m) => [...m, currentStreamRef.current]);
        } else {
          // Partes subsequentes - atualiza a última mensagem
          currentStreamRef.current.text += messageText;
          setMessages((m) => [...m.slice(0, -1), { ...currentStreamRef.current }]);
        }
      };

      wsRef.current.onclose = () => {
        setIsConnected(false);
        currentStreamRef.current = null;
        if (shouldStop) return;
        console.info('ChatAgent: WebSocket fechado. Reconectando em', reconnectDelay, 'ms...');
        setTimeout(() => {
          reconnectDelay = Math.min(30000, reconnectDelay * 2);
          connect();
        }, reconnectDelay);
      };

      wsRef.current.onerror = (error) => {
        console.error('ChatAgent: Erro no WebSocket:', error);
        setIsConnected(false);
        currentStreamRef.current = null;
        // Força fechamento para acionar reconexão
        try { wsRef.current.close(); } catch (e) {}
      };
    }

    connect();
    return () => {
      shouldStop = true;
      currentStreamRef.current = null;
      try { wsRef.current && wsRef.current.close(); } catch (e) {}
    };
  }, [wsUrl]);

  function send() {
    const text = input.trim();
    if (!text) return;
    
    if (!isConnected) {
      alert('Aguarde a conexão com o servidor...');
      return;
    }

    // Mostra imediatamente a mensagem do usuário
    setMessages((m) => [...m, { role: 'user', text }]);
    setIsTyping(true);
    
    // Reseta o stream anterior
    currentStreamRef.current = null;

    // Envia texto simples para o backend (não JSON)
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(text);
    } else {
      setIsTyping(false);
      setMessages((m) => [...m, { 
        role: 'assistant', 
        text: 'Erro: Conexão não está aberta. Tente novamente.' 
      }]);
    }
    setInput('');
  }

  return (
    <div className="flex flex-col h-full bg-white rounded-lg shadow-lg overflow-hidden">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-700 p-4 text-white flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Bot className="w-6 h-6" />
          <div>
            <h3 className="font-bold text-lg">Assistente de Documentos</h3>
            <p className="text-xs text-blue-100">
              {isConnected ? '● Conectado' : '○ Desconectado'}
            </p>
          </div>
        </div>
      </div>

      {/* Área de Mensagens */}
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
              className={`max-w-[75%] rounded-2xl px-4 py-3 ${
                m.role === 'user' 
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

      {/* Input Area */}
      <div className="border-t border-gray-200 p-4 bg-white">
        <div className="flex gap-2">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { 
              if (e.key === 'Enter' && !e.shiftKey) { 
                e.preventDefault(); 
                send(); 
              } 
            }}
            placeholder="Digite sua pergunta aqui..."
            disabled={!isConnected}
            className="flex-1 px-4 py-3 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 disabled:cursor-not-allowed"
          />
          <button 
            onClick={send}
            disabled={!isConnected || !input.trim()}
            className="bg-blue-600 text-white p-3 rounded-full hover:bg-blue-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
}