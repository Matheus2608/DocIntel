# Feature de Chat com IA - Document Q&A

## 📋 Visão Geral

Esta feature permite que os clientes façam upload de documentos (PDF, DOC, DOCX) e interajam com uma IA através de um chat em tempo real usando WebSocket.

## 🎯 Funcionalidades

1. **Upload de Documentos**: Interface drag-and-drop para upload de arquivos
2. **Chat em Tempo Real**: Comunicação via WebSocket com a IA
3. **Interface Responsiva**: Design moderno com Tailwind CSS
4. **Indicadores Visuais**: Status de conexão, typing indicators, etc.
5. **Reconexão Automática**: O WebSocket reconecta automaticamente se a conexão cair

## 🗂️ Arquivos Modificados/Criados

### 1. `/src/components/ChatAgent.jsx`
Componente principal do chat que gerencia:
- Conexão WebSocket com o backend
- Envio e recebimento de mensagens
- Estado de conexão e typing indicators
- UI moderna com animações

**Props:**
- `documentId` (opcional): ID do documento para contexto no chat

### 2. `/src/pages/DocumentQA.jsx`
Página que gerencia o fluxo completo:
- Upload de documentos
- Exibição do chat após upload bem-sucedido
- Remoção de documento e reinício do processo

### 3. `/vite.config.js`
Configuração de proxy para desenvolvimento:
- Proxy HTTP para `/api`
- Proxy WebSocket para `/document-support-agent`

### 4. `/src/App.jsx`
Atualizado para mostrar a página DocumentQA

## 🔧 Configuração do Backend

O frontend espera que o backend tenha:

### 1. Endpoint de Upload
```
POST http://localhost:8080/upload-document
Content-Type: multipart/form-data

Body: { file: <arquivo> }

Response: { 
  documentId: "string" | id: "string"
}
```

### 2. WebSocket Endpoint
```
WS ws://localhost:8080/document-support-agent?documentId=<id>
```

**Formato de Mensagem (Cliente → Servidor):**
```json
{
  "type": "question",
  "text": "Qual é o conteúdo do documento?",
  "documentId": "doc-123"
}
```

**Formato de Resposta (Servidor → Cliente):**
O componente aceita múltiplos formatos:

```json
// Formato 1
{
  "role": "assistant",
  "content": "Resposta da IA..."
}

// Formato 2
{
  "role": "assistant",
  "text": "Resposta da IA..."
}

// Formato 3
{
  "text": "Resposta da IA..."
}

// Formato 4
{
  "message": "Resposta da IA..."
}

// Formato 5 (texto simples)
"Resposta da IA..."
```

## 🚀 Como Usar

### 1. Iniciar o Servidor de Desenvolvimento

```bash
npm run dev
```

### 2. Fluxo do Usuário

1. **Upload**: Arraste um arquivo ou clique para selecionar (PDF, DOC, DOCX)
2. **Processamento**: O arquivo é enviado para o backend
3. **Chat**: Após upload bem-sucedido, a interface do chat aparece
4. **Interação**: Digite perguntas sobre o documento e receba respostas da IA
5. **Reinício**: Clique em "Remover documento" para fazer upload de outro arquivo

## 🎨 Personalização

### Alterar URL do Backend

Em `ChatAgent.jsx`, linha ~12:
```javascript
const wsUrl = `ws://SEU_BACKEND:PORTA/document-support-agent${documentId ? `?documentId=${documentId}` : ''}`;
```

Em `DocumentQA.jsx`, linha ~28:
```javascript
const response = await axios.post('http://SEU_BACKEND:PORTA/upload-document', formData, {
```

### Customizar Aparência

O componente usa Tailwind CSS. Você pode modificar as classes nos arquivos:
- `ChatAgent.jsx` - UI do chat
- `DocumentQA.jsx` - UI de upload

### Adicionar Tipos de Arquivo

Em `DocumentQA.jsx`, atualizar o `accept` do dropzone:
```javascript
accept: {
  'application/pdf': ['.pdf'],
  'application/msword': ['.doc'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
  // Adicione mais tipos aqui
}
```

## 🐛 Troubleshooting

### WebSocket não conecta
- Verifique se o backend está rodando em `localhost:8080`
- Verifique os logs do console para erros de conexão
- Confirme que a rota `/document-support-agent` está correta

### Upload falha
- Verifique se a rota `/upload-document` existe no backend
- Verifique o tamanho máximo permitido do arquivo no backend
- Olhe os logs do console para detalhes do erro

### Mensagens não aparecem
- Verifique o formato das mensagens do backend
- Abra o DevTools → Network → WS para ver mensagens do WebSocket
- Confirme que o backend está enviando no formato correto

## 📱 Recursos da Interface

- ✅ Animações suaves
- ✅ Typing indicator quando a IA está processando
- ✅ Scroll automático para novas mensagens
- ✅ Status de conexão visível
- ✅ Mensagens do usuário e IA diferenciadas visualmente
- ✅ Botões com estados disabled apropriados
- ✅ Feedback visual para upload
- ✅ Tratamento de erros

## 🔜 Próximos Passos Sugeridos

1. Adicionar sistema de rotas (React Router)
2. Salvar histórico de conversas
3. Adicionar opção de download do documento
4. Implementar autenticação de usuário
5. Adicionar suporte a múltiplos documentos
6. Implementar busca no histórico de conversas
7. Adicionar formatação Markdown nas respostas da IA
8. Implementar feedback de "útil/não útil" nas respostas

## 📝 Notas Importantes

- O WebSocket reconecta automaticamente em caso de queda
- O estado das mensagens é mantido apenas no componente (não persistido)
- O documentId é usado para contexto, mas você pode ajustar conforme sua necessidade
- A URL do WebSocket pode precisar ser ajustada em produção (usar variáveis de ambiente)

## 🔐 Segurança

Lembre-se de implementar no backend:
- Validação de tipo e tamanho de arquivo
- Sanitização de inputs
- Autenticação/Autorização
- Rate limiting no WebSocket
- Timeout para mensagens
