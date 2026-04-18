# 📄 DocIntel - Intelligent Document Assistant

<div align="center">

**Converse com seus documentos usando Inteligência Artificial**

*Faça upload de PDFs, DOCs ou TXTs e obtenha respostas instantâneas sobre o conteúdo através de um chat inteligente*

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.30-blue.svg)](https://quarkus.io/)
[![React](https://img.shields.io/badge/React-19-61DAFB.svg)](https://react.dev/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-412991.svg)](https://openai.com/)

</div>

<div align="center">
  <video controls width="900">
    <source src="./assets/docintel-demo.mp4" type="video/mp4">
    Seu navegador não suporta o elemento de vídeo.
  </video>
</div>
---

## 🎯 O que é o DocIntel?

DocIntel é uma plataforma de análise de documentos com IA que permite você fazer perguntas em linguagem natural sobre o conteúdo dos seus documentos. A aplicação utiliza técnicas avançadas de RAG (Retrieval-Augmented Generation) e reranking semântico para fornecer respostas precisas e contextualizadas.

### ✨ Recursos Principais

#### 📤 **Upload de Documentos**
- Suporte para múltiplos formatos: **PDF**, **DOCX**, **DOC**, **TXT**
- Interface drag-and-drop intuitiva
- Processamento automático e indexação vetorial
- Armazenamento seguro e persistente

#### 💬 **Chat Inteligente em Tempo Real**
- Conversação natural com IA especializada em seus documentos
- Respostas em streaming (texto aparece progressivamente)
- Comunicação via WebSocket para experiência fluida
- Histórico completo de conversas

#### 🎯 **Sistema de Reranking Avançado**
- **Dupla pontuação de relevância**:
  - Score de similaridade vetorial (embedding)
  - Score de relevância semântica (modelo GPT-4o-mini)
- **Filtragem inteligente**: apenas respostas com 80%+ de relevância
- **Ranqueamento automático**: melhores resultados aparecem primeiro
- **Transparência**: visualize os trechos usados e suas pontuações

#### 📊 **Análise de Fontes RAG**
- Visualize os trechos do documento utilizados para cada resposta
- Compare scores de embedding vs. relevância semântica
- Identificação de conteúdo filtrado por baixa relevância
- Interface visual com gráficos de pontuação

#### 🗂️ **Gerenciamento de Chats**
- Múltiplas conversas simultâneas
- Histórico persistente de mensagens
- Busca e navegação entre chats
- Associação de documentos a conversas específicas

#### 🌓 **Interface Moderna**
- Design responsivo (desktop e mobile)
- Modo escuro/claro
- Animações suaves e feedback visual
- Componentes acessíveis

---

## 🤖 Spec-Driven Development (SDD)

Este projeto está configurado para **Spec-Driven Development** usando [Tessl](https://tessl.io/), uma abordagem moderna onde especificações são escritas antes do código.

### 🎯 Por que SDD?

- **Menos alucinações de IA**: Specs do Tessl Registry explicam corretamente como usar bibliotecas
- **Documentação viva**: Specs mantêm-se sincronizadas com o código
- **Melhor colaboração**: Alinhamento claro entre desenvolvedores e agentes de IA

### 📚 Começando com Tessl

```bash
# Fazer login no Tessl (necessário para usar o registry)
tessl login

# Pesquisar specs para bibliotecas que você usa
tessl search quarkus
tessl search react

# Instalar specs no projeto
tessl install quarkus/rest
```

**📖 Documentação completa**: Veja [SDD.md](./SDD.md) para guia detalhado de Spec-Driven Development com Tessl.

---

## 🚀 Começando

### 📋 Pré-requisitos

Antes de iniciar, certifique-se de ter instalado:

#### Backend
- **Java 21** ou superior ([Download](https://www.oracle.com/java/technologies/downloads/#java21))
- **Maven 3.9+** (incluído no projeto via wrapper `./mvnw`)
- **PostgreSQL 14+** com extensão **pgvector**
- **Chave API da OpenAI** ([Obter aqui](https://platform.openai.com/api-keys))

#### Frontend
- **Node.js 18+** ([Download](https://nodejs.org/))
- **npm** ou **yarn**

### 🔧 Instalação e Configuração

#### 1️⃣ Clone o Repositório

```bash
git clone <repository-url>
cd DocIntel
```

#### 2️⃣ Configurar o Banco de Dados

**Instalar PostgreSQL e pgvector:**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo apt install postgresql-14-pgvector

# macOS (Homebrew)
brew install postgresql@14
brew install pgvector

# Ou use o script fornecido:
cd back
./setup-postgres.sh
```

**Criar o banco de dados:**

```bash
# Conectar ao PostgreSQL
sudo -u postgres psql

# Criar banco e extensão
CREATE DATABASE docintel;
\c docintel
CREATE EXTENSION vector;
\q
```

**Executar migração para reranking (novo recurso):**

```bash
cd back
psql -U postgres -d docintel -f add_reranking_columns.sql
```

#### 3️⃣ Configurar Backend

**Configurar variáveis de ambiente:**

```bash
cd back

# Criar arquivo .env ou exportar as variáveis:
export OPENAI_API_KEY="sua-chave-api-aqui"
export QUARKUS_DATASOURCE_USERNAME="postgres"
export QUARKUS_DATASOURCE_PASSWORD="sua-senha"
```

**Compilar e executar:**

```bash
# Compilar o projeto
./mvnw clean package

# Executar em modo desenvolvimento (live reload)
./mvnw quarkus:dev
```

O backend estará disponível em: **http://localhost:8080**

**Endpoints principais:**
- `http://localhost:8080/q/dev/` - Quarkus Dev UI
- `http://localhost:8080/api/chats` - API REST
- `ws://localhost:8080/document-support-agent/{chatId}` - WebSocket

#### 4️⃣ Configurar Frontend

```bash
cd front

# Instalar dependências
npm install

# Configurar variáveis de ambiente
cat > .env << EOF
VITE_API_URL=http://localhost:8080/api/chats
VITE_WS_URL=ws://localhost:8080
EOF

# Iniciar servidor de desenvolvimento
npm run dev
```

O frontend estará disponível em: **http://localhost:5173**

---

## 🐛 Troubleshooting

### Backend não inicia

**❌ Erro**: `java.sql.SQLException: Connection refused`

**✅ Solução**: Verifique se o PostgreSQL está rodando:
```bash
sudo systemctl status postgresql
sudo systemctl start postgresql
```

---

**❌ Erro**: `Extension "vector" not found`

**✅ Solução**: Instale a extensão pgvector:
```bash
sudo apt install postgresql-14-pgvector
psql -U postgres -d docintel -c "CREATE EXTENSION vector;"
```

---

**❌ Erro**: `OPENAI_API_KEY not set`

**✅ Solução**: Configure a variável de ambiente:
```bash
export OPENAI_API_KEY="sk-..."
# ou adicione ao ~/.bashrc para persistir
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.bashrc
```

---

**❌ Erro**: `Port 8080 already in use`

**✅ Solução**: Identifique e mate o processo:
```bash
lsof -i :8080
kill -9 <PID>
```

### Frontend não conecta ao backend

**❌ Erro**: `CORS error`

**✅ Solução**: Verifique `application.properties`:
```properties
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:5173
```

---

**❌ Erro**: `WebSocket connection failed`

**✅ Solução**: 
1. Verifique se o backend está rodando
2. Verifique a URL do WebSocket no `.env`:
```env
VITE_WS_URL=ws://localhost:8080
```

---

**❌ Erro**: `Failed to fetch chats`

**✅ Solução**: Verifique se a API está acessível:
```bash
curl http://localhost:8080/api/chats
```

### Problemas com Upload

**❌ Erro**: `File too large`

**✅ Solução**: Aumente o limite em `application.properties`:
```properties
quarkus.http.limits.max-body-size=50M
```

---

**❌ Erro**: `Unsupported file type`

**✅ Solução**: Verifique se o formato é suportado (PDF, DOCX, DOC, TXT)

---
## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor, siga estas diretrizes:

1. **Fork** o projeto
2. Crie uma **branch** para sua feature (`git checkout -b feature/minha-feature`)
3. **Commit** suas mudanças (`git commit -m 'Adiciona nova funcionalidade X'`)
4. **Push** para a branch (`git push origin feature/minha-feature`)
5. Abra um **Pull Request**

**Padrões de código:**
- Backend: Siga as convenções Java/Quarkus
- Frontend: Use ESLint e Prettier
- Testes: Adicione testes para novas funcionalidades
- Documentação: Atualize o README se necessário

---

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.


---

<div align="center">

**Desenvolvido com ❤️ usando Quarkus, React e OpenAI**

⭐ Se este projeto foi útil para você, considere dar uma estrela no GitHub!

</div>

