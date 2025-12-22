# 🚀 Guia Rápido - Testar Feature de Chat

## Opção 1: Com Backend Real

### Pré-requisitos
Seu backend deve ter:
- ✅ Endpoint POST `/upload-document` que aceita multipart/form-data
- ✅ WebSocket em `/document-support-agent`

### Passos

1. **Configure o backend (se necessário)**
   ```bash
   # Copie o .env.example para .env
   cp .env.example .env
   
   # Edite .env e ajuste as URLs
   VITE_BACKEND_URL=http://localhost:8080
   VITE_WS_URL=ws://localhost:8080
   VITE_USE_MOCK=false
   ```

2. **Inicie o servidor**
   ```bash
   npm run dev
   ```

3. **Acesse** `http://localhost:5173`

4. **Teste o fluxo:**
   - Faça upload de um PDF/DOC/DOCX
   - Após o upload, o chat aparecerá
   - Digite uma pergunta e pressione Enter ou clique em Enviar
   - A IA responderá via WebSocket

---

## Opção 2: Modo Mock (Sem Backend)

Para testar a interface sem precisar de backend:

1. **Crie um arquivo `.env`**
   ```bash
   echo "VITE_USE_MOCK=true" > .env
   ```

2. **Ative o mock no App.jsx**
   
   Adicione no início do arquivo `/src/App.jsx`:
   ```jsx
   import { startMockWebSocketServer } from './utils/mockWebSocket';
   
   // Logo após os imports
   if (import.meta.env.VITE_USE_MOCK === 'true') {
     startMockWebSocketServer();
   }
   ```

3. **Simule o upload**
   
   Em `/src/pages/DocumentQA.jsx`, comente a chamada real ao backend e simule sucesso:
   
   ```jsx
   // Comentar estas linhas:
   // const response = await axios.post(...)
   
   // Adicionar:
   console.log('Modo Mock: Simulando upload...');
   await new Promise(resolve => setTimeout(resolve, 1000)); // Simula delay
   setDocumentId('mock-doc-' + Date.now());
   setUploadedFile(file);
   ```

4. **Inicie o servidor**
   ```bash
   npm run dev
   ```

5. **Teste:**
   - Faça upload de qualquer arquivo
   - O chat aparecerá após 1 segundo
   - Digite mensagens e receba respostas mock da "IA"

---

## 🐛 Debug

### Ver mensagens do WebSocket

Abra DevTools (F12):
1. Vá para aba **Network**
2. Filtre por **WS** (WebSocket)
3. Clique na conexão `/document-support-agent`
4. Veja as mensagens enviadas/recebidas

### Logs do Console

O componente loga automaticamente:
- ✅ Conexões WebSocket
- ✅ Mensagens enviadas/recebidas
- ✅ Erros de conexão
- ✅ Tentativas de reconexão

Procure no console por:
- `ChatAgent: WebSocket conectado!`
- `ChatAgent: Mensagem recebida:`
- `ChatAgent: Erro no WebSocket:`

---

## 📝 Checklist de Teste

- [ ] Upload de arquivo PDF funciona
- [ ] Upload de arquivo DOC/DOCX funciona
- [ ] Chat aparece após upload bem-sucedido
- [ ] Status de conexão mostra "Conectado"
- [ ] Mensagem é enviada ao pressionar Enter
- [ ] Mensagem é enviada ao clicar no botão
- [ ] IA responde à mensagem
- [ ] Typing indicator aparece enquanto IA processa
- [ ] Scroll automático funciona
- [ ] Botão "Remover documento" funciona
- [ ] Reconexão automática funciona (tente parar o backend)
- [ ] Interface é responsiva no mobile

---

## 🎨 Teste de UI

Teste diferentes cenários:

### Mensagens longas
```
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
```

### Múltiplas mensagens
Envie várias mensagens seguidas para testar o scroll e o layout.

### Quebras de linha
```
Primeira linha
Segunda linha
Terceira linha
```

---

## ⚙️ Configurações Avançadas

### Mudar porta do Vite
```javascript
// vite.config.js
export default defineConfig({
  server: {
    port: 3000, // Mude aqui
    // ...
  }
})
```

### Ajustar timeout de reconexão
```javascript
// ChatAgent.jsx, linha ~22
let reconnectDelay = 1000; // milissegundos
```

### Desabilitar auto-reconexão
```javascript
// ChatAgent.jsx, função onclose
// Comente ou remova o setTimeout de reconexão
```

---

## 🔗 URLs Importantes

- Frontend: `http://localhost:5173` (padrão Vite)
- Backend: `http://localhost:8080` (ajuste conforme necessário)
- WebSocket: `ws://localhost:8080/document-support-agent`

---

## 💡 Dicas

1. **Use o mock** para desenvolver a UI sem depender do backend
2. **Veja os logs** do console para entender o que está acontecendo
3. **Teste a reconexão** parando e iniciando o backend
4. **Verifique o formato** das mensagens do backend no DevTools
5. **Ajuste o timeout** se a IA demorar muito para responder

---

## 🆘 Problemas Comuns

### "Erro: conexão não está aberta"
- Verifique se o backend está rodando
- Verifique a URL do WebSocket no código
- Veja os logs do console

### Upload não funciona
- Verifique se a rota `/upload-document` existe
- Confirme que o backend aceita multipart/form-data
- Veja os logs de erro no console

### IA não responde
- Abra DevTools → Network → WS
- Verifique se as mensagens estão sendo enviadas
- Confirme o formato das mensagens do backend
- Veja os logs do servidor backend

### Interface quebrada
- Execute `npm install` para garantir dependências
- Limpe o cache: `npm run build` e reinicie
- Verifique se o Tailwind CSS está configurado
