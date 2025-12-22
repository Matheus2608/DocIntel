# API de Gerenciamento de Chats e Documentos

## Estrutura do Banco de Dados

### Tabelas

1. **chats**: Armazena as sessões de chat
   - `id`: UUID único do chat
   - `created_at`: Data/hora de criação
   - `updated_at`: Data/hora da última atualização

2. **chat_messages**: Armazena mensagens de cada chat
   - `id`: UUID único da mensagem
   - `chat_id`: Referência ao chat (FK)
   - `role`: Tipo da mensagem ('user' ou 'assistant')
   - `content`: Conteúdo da mensagem
   - `created_at`: Data/hora de criação

3. **document_files**: Armazena arquivos associados aos chats
   - `id`: UUID único do arquivo
   - `chat_id`: Referência ao chat (FK, único)
   - `file_name`: Nome do arquivo
   - `file_type`: Tipo MIME do arquivo
   - `file_size`: Tamanho em bytes
   - `file_data`: Conteúdo binário do arquivo
   - `uploaded_at`: Data/hora do upload

### Relacionamentos

- Um **chat** pode ter várias **mensagens** (1:N)
- Um **chat** pode ter **um único documento** (1:1)
- Ao deletar um chat, todas as mensagens e o documento associado são removidos (CASCADE)

## Endpoints da API

### Base URL
```
http://localhost:8080/api/chats
```

### 1. Criar um novo chat

```bash
POST /api/chats
```

**Resposta:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "createdAt": "2025-12-21T10:30:00",
  "updatedAt": "2025-12-21T10:30:00",
  "hasDocument": false
}
```

**Exemplo curl:**
```bash
curl -X POST http://localhost:8080/api/chats
```

---

### 2. Listar todos os chats

```bash
GET /api/chats
```

**Resposta:**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "createdAt": "2025-12-21T10:30:00",
    "updatedAt": "2025-12-21T10:30:00",
    "hasDocument": true
  }
]
```

**Exemplo curl:**
```bash
curl http://localhost:8080/api/chats
```

---

### 3. Obter um chat específico

```bash
GET /api/chats/{chatId}
```

**Resposta:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "createdAt": "2025-12-21T10:30:00",
  "updatedAt": "2025-12-21T10:30:00",
  "hasDocument": true
}
```

**Exemplo curl:**
```bash
curl http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000
```

---

### 4. Deletar um chat

```bash
DELETE /api/chats/{chatId}
```

**Resposta:** 204 No Content

**Exemplo curl:**
```bash
curl -X DELETE http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000
```

---

### 5. Upload de documento (PDF ou DOCX)

```bash
POST /api/chats/{chatId}/document
Content-Type: multipart/form-data
```

**Parâmetros:**
- `file`: Arquivo PDF ou DOCX

**Resposta:**
```json
{
  "id": "456e7890-e89b-12d3-a456-426614174000",
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "fileSize": 1024000,
  "uploadedAt": "2025-12-21T10:35:00"
}
```

**Exemplo curl:**
```bash
curl -X POST \
  -F "file=@/caminho/para/documento.pdf" \
  http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000/document
```

**Tipos de arquivo aceitos:**
- `application/pdf` (PDF)
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (DOCX)
- `application/msword` (DOC)

**Exemplo JavaScript (Frontend):**
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch(`http://localhost:8080/api/chats/${chatId}/document`, {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => console.log('Upload bem-sucedido:', data))
.catch(error => console.error('Erro:', error));
```

---

### 6. Obter informações do documento

```bash
GET /api/chats/{chatId}/document
```

**Resposta:**
```json
{
  "id": "456e7890-e89b-12d3-a456-426614174000",
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "fileSize": 1024000,
  "uploadedAt": "2025-12-21T10:35:00"
}
```

**Exemplo curl:**
```bash
curl http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000/document
```

---

### 7. Baixar documento

```bash
GET /api/chats/{chatId}/document/download
```

**Resposta:** Arquivo binário (PDF ou DOCX)

**Exemplo curl:**
```bash
curl -O -J http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000/document/download
```

---

### 8. Adicionar mensagem ao chat

```bash
POST /api/chats/{chatId}/messages
Content-Type: application/json
```

**Body:**
```json
{
  "role": "user",
  "content": "Qual é o conteúdo do documento?"
}
```

**Resposta:**
```json
{
  "id": "789e1234-e89b-12d3-a456-426614174000",
  "role": "user",
  "content": "Qual é o conteúdo do documento?",
  "createdAt": "2025-12-21T10:40:00"
}
```

**Exemplo curl:**
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"role":"user","content":"Qual é o conteúdo do documento?"}' \
  http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000/messages
```

---

### 9. Listar mensagens do chat

```bash
GET /api/chats/{chatId}/messages
```

**Resposta:**
```json
[
  {
    "id": "789e1234-e89b-12d3-a456-426614174000",
    "role": "user",
    "content": "Qual é o conteúdo do documento?",
    "createdAt": "2025-12-21T10:40:00"
  },
  {
    "id": "890e2345-e89b-12d3-a456-426614174000",
    "role": "assistant",
    "content": "O documento contém...",
    "createdAt": "2025-12-21T10:40:05"
  }
]
```

**Exemplo curl:**
```bash
curl http://localhost:8080/api/chats/123e4567-e89b-12d3-a456-426614174000/messages
```

---

## Fluxo de Uso Típico

1. **Criar um novo chat:**
   ```bash
   POST /api/chats
   # Retorna: chatId
   ```

2. **Upload de documento:**
   ```bash
   POST /api/chats/{chatId}/document
   # Enviar arquivo PDF ou DOCX
   ```

3. **Adicionar mensagem do usuário:**
   ```bash
   POST /api/chats/{chatId}/messages
   # Body: {"role": "user", "content": "Pergunta sobre o documento"}
   ```

4. **Adicionar resposta do assistente:**
   ```bash
   POST /api/chats/{chatId}/messages
   # Body: {"role": "assistant", "content": "Resposta da IA"}
   ```

5. **Listar histórico de mensagens:**
   ```bash
   GET /api/chats/{chatId}/messages
   ```

---

## Tratamento de Erros

### 400 Bad Request
Tipo de arquivo inválido:
```json
{
  "message": "Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos."
}
```

### 404 Not Found
Chat ou documento não encontrado:
```json
{
  "message": "Chat not found"
}
```

### 500 Internal Server Error
Erro ao processar arquivo:
```json
{
  "message": "Erro ao processar o arquivo: [detalhes do erro]"
}
```

---

## CORS

A API está configurada para aceitar requisições do frontend em `http://localhost:5173`.

Para modificar, edite o arquivo `application.properties`:
```properties
quarkus.http.cors.origins=http://localhost:5173
```

---

## Configuração do Banco de Dados

Edite `src/main/resources/application.properties`:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=matheus
quarkus.datasource.password=tryhackme
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/meu_banco

# Hibernate ORM - Em desenvolvimento, recria as tabelas ao iniciar
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true
```

**Nota:** Em produção, altere `database.generation` para `update` ou `validate`.

