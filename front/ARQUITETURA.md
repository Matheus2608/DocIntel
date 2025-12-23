# Arquitetura Refatorada - DocIntel Frontend

## 📁 Estrutura de Pastas

```
src/
├── hooks/
│   └── useWebSocketChat.js        # Hook customizado para WebSocket
├── components/
│   ├── ChatMessage.jsx            # Componente de apresentação (apenas UI)
│   ├── InputMessage.jsx           # Componente controlado (gerencia próprio estado)
│   ├── Upload.jsx                 # Componente de upload
│   └── Header.jsx                 # Componente de cabeçalho
└── pages/
    └── home/
        └── Main.jsx               # Página principal (orquestrador)
```

## 🎯 Princípios Aplicados

### 1. **Separação de Responsabilidades**
Cada componente tem uma única responsabilidade clara:

- **Main.jsx**: Orquestrador principal
  - Gerencia estado do chat atual
  - Coordena comunicação entre componentes
  - Decide o que renderizar (Upload ou Chat)

- **ChatMessage.jsx**: Componente de apresentação
  - Apenas exibe mensagens
  - Implementa auto-scroll
  - Não gerencia estado ou lógica de negócio

- **InputMessage.jsx**: Componente controlado
  - Gerencia seu próprio input
  - Valida antes de enviar
  - Notifica o pai via callback

- **useWebSocketChat.js**: Hook customizado
  - Encapsula toda lógica WebSocket
  - Gerencia estado de conexão e mensagens
  - Implementa streaming e reconexão

### 2. **Fluxo de Dados Unidirecional**

```
Main (estado global)
  ↓
  ├─→ Upload → onUploadSuccess → Main (atualiza currentChat)
  ├─→ ChatMessage (recebe messages, isTyping)
  └─→ InputMessage → onSendMessage → useWebSocketChat → atualiza messages
```

### 3. **Composição sobre Herança**

Componentes pequenos e reutilizáveis que podem ser compostos:
```jsx
<Main>
  <Header />
  {currentChat ? <ChatMessage /> : <Upload />}
  <InputMessage />
</Main>
```

## 🔧 Componentes Detalhados

### Main.jsx (Orquestrador)
**Responsabilidades:**
- Gerenciar estado do chat atual (`currentChat`)
- Usar o hook `useWebSocketChat` para WebSocket e mensagens
- Coordenar upload e início do chat
- Renderizar condicionalmente Upload ou ChatMessage

**Props:**
- `isDarkMode`: Tema visual

**Estado:**
- `currentChat`: Dados do chat atual (null = sem upload)

**Hooks:**
- `useWebSocketChat(chatId, wsUrl)`: Retorna { messages, isConnected, isTyping, sendMessage, clearMessages }

---

### ChatMessage.jsx (Apresentação)
**Responsabilidades:**
- Renderizar lista de mensagens
- Mostrar indicador de digitação
- Auto-scroll para última mensagem

**Props:**
- `messages`: Array de mensagens
- `isTyping`: Booleano para indicador de digitação

**Não tem:** Estado local, lógica de negócio, side effects complexos

---

### InputMessage.jsx (Controlado)
**Responsabilidades:**
- Gerenciar seu próprio input de texto
- Validar entrada antes de enviar
- Notificar pai quando usuário envia mensagem
- Lidar com tecla Enter

**Props:**
- `isDarkMode`: Tema visual
- `isConnected`: Estado de conexão do WebSocket
- `onSendMessage`: Callback para enviar mensagem

**Estado Local:**
- `input`: Texto digitado pelo usuário

**Fluxo:**
1. Usuário digita → `setInput()`
2. Usuário pressiona Enter ou clica enviar
3. Valida se tem texto e conexão
4. Chama `onSendMessage(text)`
5. Se retornar `true`, limpa o input

---

### Upload.jsx (Controlado)
**Responsabilidades:**
- Upload de arquivos (drag & drop + clique)
- Validar tipo de arquivo
- Chamar API de criação de chat
- Mostrar loading e erros

**Props:**
- `onUploadSuccess`: Callback com dados do chat criado

**Estado Local:**
- `isDragging`: Estado do drag & drop
- `isUploading`: Loading
- `error`: Mensagem de erro

---

### useWebSocketChat.js (Hook Customizado)
**Responsabilidades:**
- Conectar ao WebSocket quando há chatId
- Gerenciar estado de conexão
- Receber mensagens (incluindo streaming)
- Enviar mensagens
- Reconexão automática

**Parâmetros:**
- `chatId`: ID do chat (null = não conecta)
- `wsUrl`: URL do WebSocket

**Retorno:**
```javascript
{
  messages: [],              // Array de mensagens
  isConnected: false,        // Estado da conexão
  isTyping: false,           // Assistente digitando
  sendMessage: (text) => {}, // Função para enviar
  clearMessages: () => {}    // Limpar mensagens
}
```

**Lógica Interna:**
- Refs: `wsRef`, `currentStreamRef`
- Efeito que conecta/desconecta baseado em `chatId`
- Handlers para: open, message, close, error
- Streaming: acumula chunks da resposta

## 🔄 Fluxo Completo

### 1. Inicialização
```
Main renderiza
  → currentChat = null
  → useWebSocketChat(null, wsUrl)
    → Não conecta (sem chatId)
  → Renderiza <Upload />
```

### 2. Upload de Documento
```
Usuário seleciona arquivo
  → Upload valida tipo
  → POST /api/chats
  → Backend retorna ChatResponse
  → onUploadSuccess(chatData)
  → Main atualiza currentChat
  → useWebSocketChat(chatId, wsUrl)
    → Conecta ao WebSocket
    → Recebe boas-vindas
  → Renderiza <ChatMessage />
```

### 3. Envio de Mensagem
```
Usuário digita em InputMessage
  → input local atualizado
Usuário pressiona Enter
  → InputMessage.handleSend()
  → Valida texto e conexão
  → onSendMessage(text)
    → useWebSocketChat.sendMessage(text)
      → Adiciona mensagem do usuário
      → Envia via WebSocket
      → setIsTyping(true)
  → InputMessage limpa input
```

### 4. Recebimento de Resposta (Streaming)
```
Backend envia chunks via WebSocket
  → useWebSocketChat.onmessage
  → Primeiro chunk:
    → Cria nova mensagem do assistente
    → setIsTyping(false)
  → Chunks subsequentes:
    → Atualiza última mensagem
  → ChatMessage re-renderiza com nova mensagem
  → Auto-scroll para o fim
```

## ✅ Vantagens da Nova Arquitetura

### 1. **Manutenibilidade**
- Cada arquivo tem responsabilidade única
- Fácil localizar onde fazer mudanças
- Componentes pequenos e testáveis

### 2. **Reusabilidade**
- `ChatMessage` pode ser usado em qualquer lugar
- `InputMessage` é independente
- `useWebSocketChat` pode ser usado em outras páginas

### 3. **Testabilidade**
- Hook pode ser testado isoladamente
- Componentes de apresentação são puros
- Lógica de negócio separada da UI

### 4. **Performance**
- Menos re-renders desnecessários
- Estado local onde faz sentido
- Memoização mais fácil se necessário

### 5. **Escalabilidade**
- Fácil adicionar novas features
- Padrão claro para novos componentes
- Hook pode ser estendido

## 🎨 Padrões de Design Utilizados

1. **Container/Presenter Pattern**
   - Main.jsx = Container (lógica)
   - ChatMessage.jsx = Presenter (UI)

2. **Custom Hooks Pattern**
   - useWebSocketChat encapsula lógica complexa

3. **Controlled Components**
   - InputMessage gerencia seu próprio estado
   - Upload gerencia seu próprio estado

4. **Callback Pattern**
   - Comunicação filho → pai via callbacks
   - onUploadSuccess, onSendMessage

5. **Composition Pattern**
   - Componentes pequenos compostos em Main

## 🚀 Próximas Melhorias

1. **Context API**: Para estado global (tema, usuário)
2. **React Query**: Para gerenciar chamadas API
3. **Error Boundaries**: Para tratamento de erros
4. **Lazy Loading**: Para componentes grandes
5. **TypeScript**: Para type safety
6. **Testes**: Unit tests para hook e componentes
