# Implementação de Upload e Chat com WebSocket

## Resumo das Mudanças

Foi implementado o fluxo completo de upload de documentos e chat com integração ao backend Java.

## Fluxo de Funcionamento

### 1. Estado Inicial (Sem Documento)
- O componente `Upload` é exibido
- O WebSocket **NÃO** está conectado
- O usuário não pode enviar mensagens
- Componente `ChatMessage` não é renderizado

### 2. Upload de Documento
Quando o usuário faz upload de um arquivo:

1. **Validação**: Verifica se o arquivo é PDF, DOC ou DOCX
2. **Envio para API**: 
   ```
   POST /api/chats?title={nome_do_arquivo}
   Content-Type: multipart/form-data
   Body: FormData com o arquivo
   ```
3. **Resposta do Backend**:
   ```json
   {
     "id": "uuid-do-chat",
     "createdAt": "2025-12-22T10:30:00",
     "updatedAt": "2025-12-22T10:30:00",
     "hasDocument": true
   }
   ```

### 3. Chat Ativo (Após Upload)
- O componente `ChatMessage` é renderizado
- WebSocket se conecta automaticamente a `/document-support-agent`
- Mensagem de boas-vindas é recebida: "Welcome to DocIntel! How can I help you today?"
- Usuário pode enviar mensagens

### 4. Streaming de Respostas
O backend retorna `Multi<String>` que envia múltiplos chunks:
- Primeiro chunk: cria nova mensagem do assistente
- Chunks subsequentes: atualiza a mensagem existente (efeito de digitação)

## Componentes Modificados

### 1. `/src/components/Upload.jsx`
**Novas funcionalidades:**
- Upload via clique ou drag & drop
- Validação de tipo de arquivo
- Loading state durante upload
- Tratamento de erros
- Callback `onUploadSuccess` após upload bem-sucedido

### 2. `/src/components/ChatMessage.jsx`
**Mudanças importantes:**
- Recebe props do componente pai (estado gerenciado externamente)
- Só conecta ao WebSocket quando há um `chatId`
- Props recebidas:
  - `chatId`: ID do chat criado
  - `wsUrl`: URL do WebSocket
  - `isConnected`, `setIsConnected`: Estado de conexão
  - `isTyping`, `setIsTyping`: Estado de digitação
  - `messages`, `setMessages`: Array de mensagens
  - `currentStreamRef`: Ref para acumular streaming
  - `wsRef`: Ref do WebSocket

### 3. `/src/pages/home/Main.jsx`
**Gerenciamento de estado:**
- `currentChat`: Dados do chat atual (null quando não há upload)
- `messages`: Array de mensagens
- `isConnected`: Estado da conexão WebSocket
- `isTyping`: Indicador de que o assistente está respondendo
- `wsRef` e `currentStreamRef`: Refs compartilhadas

**Renderização condicional:**
```jsx
{currentChat ? (
  <ChatMessage {...props} />
) : (
  <Upload onUploadSuccess={handleUploadSuccess} />
)}
```

## Integração com Backend Java

### Endpoint de Upload
```java
@POST
@Path("/api/chats")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response createChat(
    @RestForm("file") FileUpload file, 
    @PathParam("title") String title
)
```

**Nota**: O código Java usa `@PathParam("title")` mas não há `@Path("/{title}")` definido. 
Atualmente o frontend envia como query parameter: `?title=nome`. 
Se o backend for atualizado, ajustar para: `/api/chats/${title}`

### WebSocket
```java
@WebSocket(path = "/document-support-agent")
public class DocumentSupportAgentWebSocket {
    
    @OnOpen
    public String onOpen() {
        return "Welcome to DocIntel! How can I help you today?";
    }
    
    @OnTextMessage
    public Multi<String> onTextMessage(String message) {
        // Retorna streaming de chunks
    }
}
```

## Variáveis de Ambiente

Adicione ao arquivo `.env`:
```bash
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

## Fluxo de Dados

```
1. Usuário seleciona arquivo
   ↓
2. Upload.jsx → POST /api/chats
   ↓
3. Backend cria chat e retorna ChatResponse
   ↓
4. Main.jsx recebe dados via handleUploadSuccess
   ↓
5. Main.jsx atualiza currentChat
   ↓
6. ChatMessage é renderizado com chatId
   ↓
7. WebSocket conecta automaticamente
   ↓
8. Recebe mensagem de boas-vindas
   ↓
9. Usuário pode enviar mensagens
   ↓
10. Backend responde com Multi<String>
   ↓
11. Frontend acumula chunks em tempo real
```

## Tratamento de Erros

### Upload
- Arquivo inválido: Mensagem de erro exibida no componente
- Erro de rede: Mensagem de erro exibida
- Erro do servidor: Mensagem de erro do backend

### WebSocket
- Erro de conexão: Reconexão automática com backoff exponencial
- Mensagens de erro do guardrail: Detectadas e exibidas
- Erros gerais: Mensagem genérica exibida

## Próximos Passos

1. **Persistir chat ID**: Armazenar em localStorage ou contexto global
2. **Histórico de chats**: Integrar com Sidebar para listar chats anteriores
3. **Salvar mensagens**: Chamar `POST /api/chats/{chatId}/messages` para persistir
4. **Carregar mensagens antigas**: `GET /api/chats/{chatId}/messages` ao abrir chat
5. **Download de documento**: `GET /api/chats/{chatId}/document/download`
