/**
 * Servidor Mock para testar o chat sem backend
 * 
 * Para usar: importe e inicialize no App.jsx
 * import { startMockWebSocketServer } from './utils/mockWebSocket';
 * startMockWebSocketServer();
 */

class MockWebSocketServer {
  constructor() {
    this.connections = [];
    this.isRunning = false;
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    
    // Sobrescrever WebSocket global
    const originalWebSocket = window.WebSocket;
    const self = this;

    window.WebSocket = function(url) {
      console.log('Mock WebSocket: Nova conexão para', url);
      
      const mock = {
        readyState: 1, // OPEN
        url: url,
        
        send: function(data) {
          console.log('Mock WebSocket: Mensagem recebida:', data);
          try {
            const message = JSON.parse(data);
            // Simular delay da IA
            setTimeout(() => {
              const response = self.generateResponse(message);
              if (this.onmessage) {
                this.onmessage({ data: JSON.stringify(response) });
              }
            }, 1000 + Math.random() * 2000);
          } catch (e) {
            console.error('Erro ao processar mensagem:', e);
          }
        },
        
        close: function() {
          console.log('Mock WebSocket: Conexão fechada');
          this.readyState = 3; // CLOSED
          if (this.onclose) {
            this.onclose();
          }
        }
      };

      // Simular abertura de conexão
      setTimeout(() => {
        if (mock.onopen) {
          mock.onopen();
        }
      }, 100);

      self.connections.push(mock);
      return mock;
    };

    // Manter referência para possível restauração
    window.WebSocket._original = originalWebSocket;
    
    console.log('🚀 Mock WebSocket Server iniciado!');
  }

  stop() {
    if (!this.isRunning) return;
    
    if (window.WebSocket._original) {
      window.WebSocket = window.WebSocket._original;
      delete window.WebSocket._original;
    }
    
    this.isRunning = false;
    this.connections = [];
    console.log('🛑 Mock WebSocket Server parado');
  }

  generateResponse(message) {
    const responses = [
      {
        role: 'assistant',
        content: 'Baseado no documento que você enviou, posso responder que...'
      },
      {
        role: 'assistant',
        content: 'Excelente pergunta! De acordo com o conteúdo do documento, a resposta é...'
      },
      {
        role: 'assistant',
        content: 'Encontrei essa informação no documento. Veja a explicação detalhada...'
      },
      {
        role: 'assistant',
        content: 'O documento menciona especificamente que...'
      },
      {
        role: 'assistant',
        content: 'Analisando o conteúdo fornecido, posso concluir que...'
      }
    ];

    const randomResponse = responses[Math.floor(Math.random() * responses.length)];
    
    // Adicionar contexto da pergunta na resposta
    if (message.text) {
      randomResponse.content += `\n\nSobre sua pergunta "${message.text}", posso dizer que este é um tema importante abordado no documento.`;
    }

    return randomResponse;
  }
}

// Instância singleton
let mockServer = null;

export function startMockWebSocketServer() {
  if (!mockServer) {
    mockServer = new MockWebSocketServer();
  }
  mockServer.start();
  return mockServer;
}

export function stopMockWebSocketServer() {
  if (mockServer) {
    mockServer.stop();
  }
}

// Auto-iniciar em desenvolvimento se VITE_USE_MOCK estiver definido
if (import.meta.env.VITE_USE_MOCK === 'true') {
  console.log('🎭 Modo Mock ativado via variável de ambiente');
  startMockWebSocketServer();
}
