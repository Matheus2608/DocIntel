# DocIntel Frontend

Interface de chat para fazer perguntas sobre documentos PDF e Word usando IA.

## 🚀 Tecnologias

- **React 19** + **Vite** - Framework e build tool
- **Tailwind CSS** - Estilização
- **Lucide React** - Ícones
- **WebSocket** - Comunicação em tempo real com IA

## 📁 Estrutura do Projeto

```
src/
├── components/       # Componentes UI
│   ├── ChatMessage.jsx
│   ├── Header.jsx
│   ├── InputMessage.jsx
│   ├── Main.jsx
│   ├── Sidebar.jsx
│   └── Upload.jsx
├── hooks/           # Hooks customizados
│   ├── useChats.js
│   └── useWebSocketChat.js
├── config.js        # Configurações centralizadas
├── App.jsx
└── main.jsx
```

## 🔧 Configuração

1. Instale as dependências:
```bash
npm install
```

2. Configure as variáveis de ambiente criando um arquivo `.env`:
```bash
VITE_API_URL=http://localhost:8080/api/chats
VITE_WS_URL=ws://localhost:8080
```

3. Inicie o servidor de desenvolvimento:
```bash
npm run dev
```

## 🎯 Funcionalidades

- ✅ Upload de documentos (PDF, DOCX, TXT)
- ✅ Chat em tempo real com IA via WebSocket
- ✅ Histórico de conversas
- ✅ Múltiplos chats simultâneos
- ✅ Modo escuro/claro
- ✅ Streaming de respostas da IA

## 📦 Scripts Disponíveis

- `npm run dev` - Inicia servidor de desenvolvimento
- `npm run build` - Build para produção
- `npm run preview` - Preview do build de produção
- `npm run lint` - Executa linter

## 🏗️ Arquitetura

### Componentes

- **App.jsx** - Componente raiz, gerencia estado global
- **Sidebar** - Lista de chats e navegação
- **Main** - Área principal (upload ou chat)
- **ChatMessage** - Renderiza mensagens do chat
- **Upload** - Drag & drop de arquivos
- **InputMessage** - Campo de entrada de mensagens
- **Header** - Cabeçalho com título do chat

### Hooks Customizados

- **useChats** - Gerencia lista de chats do backend
- **useWebSocketChat** - Gerencia conexão WebSocket e mensagens

### Configuração

O arquivo `config.js` centraliza todas as configurações de URLs e funções utilitárias:
- URLs da API REST e WebSocket
- Helper para construir URLs do WebSocket
- Classes CSS para modo escuro

## 🔄 Fluxo de Dados

1. Usuário faz upload de documento
2. Backend cria um chat e retorna ID
3. Frontend conecta ao WebSocket usando o chat ID
4. Usuário envia mensagens via WebSocket
5. IA responde em streaming
6. Mensagens são armazenadas e persistidas

## React Compiler

Este projeto usa o React Compiler para otimizações automáticas. Veja [documentação](https://react.dev/learn/react-compiler) para mais informações.
