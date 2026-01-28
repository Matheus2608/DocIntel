# Melhorias de Logging - DocIntel

## Resumo
Foram adicionados logs estratégicos em toda a aplicação para melhorar a rastreabilidade e observabilidade, sem exageros.

## Arquivos Modificados

### 1. **ChatResource** (`resource/ChatResource.java`)
- ✅ Log detalhado na criação de chat com try-catch
- ✅ Log de request/response em todos os endpoints
- ✅ Logs de operações de listagem, obtenção e deleção
- ✅ Contexto adicional: fileName, chatId, contentType

**Exemplos de logs adicionados:**
```
INFO: Received chat creation request: fileName=documento.pdf, contentType=application/pdf
INFO: Chat created successfully: chatId=abc123, fileName=documento.pdf
DEBUG: Received request to list all chats
DEBUG: Returning 5 chats
```

### 2. **RagRetrievalResource** (`resource/RagRetrievalResource.java`)
- ✅ Logger adicionado à classe
- ✅ Log de entrada e saída do endpoint de retrieval
- ✅ Contexto: messageId

**Exemplos:**
```
INFO: Received retrieval request: messageId=msg456
DEBUG: Retrieval info returned successfully: messageId=msg456
```

### 3. **ChatService** (`service/ChatService.java`)
- ✅ Try-catch completo no createChat com logging de erros
- ✅ Log de conclusão de ingestion RAG
- ✅ Logs detalhados na deleção de chat e embeddings
- ✅ Log do fluxo de saveEmptyRetrievalInfoIfNeeded

**Exemplos:**
```
INFO: Creating chat with document: fileName=manual.pdf, fileType=application/pdf, size=1048576 bytes
INFO: Chat created successfully: chatId=xyz789, documentId=doc123, title=manual
INFO: Starting RAG ingestion for file=manual.pdf
INFO: RAG ingestion completed for file=manual.pdf
DEBUG: Deleting chat and associated document: fileName=manual.pdf
INFO: All embeddings deleted for fileName=manual.pdf
```

### 4. **RetrievalInfoService** (`service/RetrievalInfoService.java`)
- ✅ **Medição de performance** no retrieveRelevantContents (tempo em ms)
- ✅ Log de estratégia de busca utilizada
- ✅ Log de quantidade de matches em cada etapa
- ✅ Rastreamento de processamento de segmentos
- ✅ Logs nas buscas paralelas (HYPOTHETICAL_QUESTIONS vs FAKE_ANSWERS)

**Exemplos:**
```
INFO: Starting RAG retrieval - chatId=abc123, question='Como funciona?', strategy=BOTH
INFO: Search params - minSimilarity=0.75, minScore=0.60, maxResults=5
DEBUG: Search returned 15 raw matches
DEBUG: Processed into 12 segments
INFO: RAG retrieval completed in 1250ms - returned 5 filtered segments
DEBUG: Adding HYPOTHETICAL_QUESTIONS strategy to search
DEBUG: Adding FAKE_ANSWERS strategy to search
DEBUG: Search strategies completed, found 15 total matches
```

### 5. **HypotheticalQuestionService** (`service/HypotheticalQuestionService.java`)
- ✅ **Medição de performance** na ingestão (tempo total em ms)
- ✅ Log de tipo de documento (PDF vs texto)
- ✅ Log de quantidade de páginas, parágrafos e segmentos
- ✅ Melhor rastreamento do processamento paralelo
- ✅ Informações sobre quantidade de questões geradas

**Exemplos:**
```
INFO: Starting document ingestion - fileName=guia.pdf, fileType=application/pdf, size=2097152 bytes
DEBUG: Processing as PDF: guia.pdf
DEBUG: Document parsed, text length: 45678 chars
INFO: Document split into 25 paragraphs
INFO: Generating embeddings for 175 segments
INFO: Parallel processing completed - generated 150 questions from 25 paragraphs for fileName=guia.pdf
INFO: Document ingestion completed in 45000ms - fileName=guia.pdf, segments=175
```

### 6. **DocumentIngestionService** (`service/DocumentIngestionService.java`)
- ✅ **Medição de performance** no parsing de PDF
- ✅ Log de contagem de páginas e tabelas
- ✅ Log de tamanho do texto extraído
- ✅ Logs de cada etapa: extração de tabelas, texto, normalização

**Exemplos:**
```
INFO: Starting PDF parsing - filename=manual.pdf, size=1048576 bytes
INFO: PDF loaded successfully - pages=50
DEBUG: Extracting table regions and tables
INFO: Tables extracted - found 5 tables
DEBUG: Text extracted - length=123456 chars
INFO: PDF parsing completed in 3500ms - filename=manual.pdf, finalLength=125000 chars
```

### 7. **DocumentSupportAgentWebSocket** (`DocumentSupportAgentWebSocket.java`)
- ✅ Logs mais detalhados do fluxo de mensagens
- ✅ Log de persistência de mensagens do usuário e assistente
- ✅ Log de quantidade de chunks e tamanho da resposta da IA
- ✅ Contexto adicional: connectionId, messageId, responseLength, chunks

**Exemplos:**
```
INFO: Processing WebSocket message - chatId=abc123, connectionId=conn789, messageLength=50
INFO: User message persisted - chatId=abc123, messageId=msg123
DEBUG: Sending message to AI agent - chatId=abc123
INFO: AI response received - chatId=abc123, responseLength=450, chunks=15
DEBUG: Assistant message persisted - messageId=msg124
INFO: WebSocket connection closed - chatId=abc123, connectionId=conn789
```

### 8. **HypotheticalQuestionRetriever** (`service/retrieval/HypotheticalQuestionRetriever.java`)
- ✅ Log dos parâmetros de busca
- ✅ Log de resultados brutos vs filtrados
- ✅ Rastreamento da estratégia de questões hipotéticas

**Exemplos:**
```
DEBUG: HypotheticalQuestion search - filename=doc.pdf, maxResults=5, minSimilarity=0.75
DEBUG: Raw search returned 10 results
DEBUG: After filtering with PARAGRAPH_KEY: 5 results
```

### 9. **FakeAnswerRetriever** (`service/retrieval/FakeAnswerRetriever.java`)
- ✅ Log da geração de resposta falsa (fake answer)
- ✅ Log do tamanho e preview da fake answer
- ✅ Log de resultados antes e depois da filtragem

**Exemplos:**
```
DEBUG: FakeAnswer search - filename=doc.pdf, maxResults=5, minSimilarity=0.75
DEBUG: Generating fake answer for question: Como funciona o sistema?
DEBUG: Fake answer generated (length=250): O sistema funciona através de...
DEBUG: Raw search returned 8 results
DEBUG: After filtering without PARAGRAPH_KEY: 4 results
```

### 10. **RetrievalSegmentProcessor** (`service/retrieval/RetrievalSegmentProcessor.java`)
- ✅ Log de quantidade de segmentos em cada etapa
- ✅ Log de deduplicação
- ✅ Log de filtragem e ordenação

**Exemplos:**
```
DEBUG: Processing 15 matches for scoring
DEBUG: Processed into 12 unique segments after deduplication
DEBUG: Filtering and sorting 12 segments with minScore=0.60, maxResults=5
DEBUG: Returning 5 filtered segments
```

## Níveis de Log Utilizados

### INFO
- Operações principais do sistema
- Criação/deleção de recursos
- Início e fim de processos importantes
- Medições de performance (tempo de execução)

### DEBUG
- Detalhes de processamento interno
- Quantidade de registros processados
- Fluxo interno de dados
- Contexto adicional para troubleshooting

### WARN
- Situações anormais mas não críticas
- Recursos não encontrados
- Operações que falharam mas têm fallback

### ERROR
- Exceções e falhas críticas
- Sempre com contexto (chatId, fileName, etc.)
- Stack trace incluído

## Benefícios Implementados

1. **Rastreabilidade Completa**: É possível seguir o fluxo de uma requisição do começo ao fim
2. **Medição de Performance**: Tempos de execução registrados em operações críticas
3. **Debugging Facilitado**: Contexto suficiente para identificar problemas rapidamente
4. **Observabilidade de IA**: Logs das estratégias de retrieval e respostas da IA
5. **Contexto Rico**: Cada log inclui IDs relevantes (chatId, messageId, fileName)
6. **Sem Excesso**: Logs estratégicos, não em cada linha de código

## Como Usar

### Desenvolvimento Local
```bash
# Ver todos os logs
./mvnw quarkus:dev

# Filtrar logs de uma classe específica
./mvnw quarkus:dev | grep ChatService

# Ver apenas INFO e acima
./mvnw quarkus:dev -Dquarkus.log.level=INFO
```

### Produção
Configure o `application.properties`:
```properties
# Nível geral
quarkus.log.level=INFO

# Nível específico por pacote
quarkus.log.category."dev.matheus.service".level=DEBUG
quarkus.log.category."dev.matheus.resource".level=INFO

# Formato
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{2.}] (%t) %s%e%n
```

## Exemplos de Fluxo Completo

### Criação de Chat com Documento
```
INFO: Received chat creation request: fileName=manual.pdf, contentType=application/pdf
DEBUG: File read successfully: 1048576 bytes
INFO: Creating chat with document: fileName=manual.pdf, fileType=application/pdf, size=1048576 bytes
DEBUG: Generated chat title: manual
INFO: Chat created successfully: chatId=abc123, documentId=doc456, title=manual
INFO: Starting RAG ingestion for file=manual.pdf
INFO: Starting document ingestion - fileName=manual.pdf, fileType=application/pdf, size=1048576 bytes
INFO: PDF loaded successfully - pages=50
INFO: Tables extracted - found 5 tables
INFO: PDF parsing completed in 3500ms - filename=manual.pdf, finalLength=125000 chars
INFO: Document split into 25 paragraphs
INFO: Parallel processing completed - generated 150 questions from 25 paragraphs
INFO: Document ingestion completed in 45000ms - fileName=manual.pdf, segments=175
INFO: RAG ingestion completed for file=manual.pdf
INFO: Chat created successfully: chatId=abc123, fileName=manual.pdf
```

### Pergunta via WebSocket com RAG
```
INFO: Processing WebSocket message - chatId=abc123, connectionId=conn789, messageLength=35
INFO: User message persisted - chatId=abc123, messageId=msg123
DEBUG: Sending message to AI agent - chatId=abc123
INFO: Starting RAG retrieval - chatId=abc123, question='Como funciona?', strategy=BOTH
DEBUG: Adding HYPOTHETICAL_QUESTIONS strategy to search
DEBUG: Adding FAKE_ANSWERS strategy to search
DEBUG: HypotheticalQuestion search - filename=manual.pdf, maxResults=5, minSimilarity=0.75
DEBUG: Raw search returned 10 results
DEBUG: FakeAnswer search - filename=manual.pdf, maxResults=5, minSimilarity=0.75
DEBUG: Fake answer generated (length=250): O sistema funciona através de...
DEBUG: Search strategies completed, found 15 total matches
DEBUG: Processing 15 matches for scoring
DEBUG: Processed into 12 unique segments after deduplication
INFO: RAG retrieval completed in 1250ms - returned 5 filtered segments
INFO: AI response received - chatId=abc123, responseLength=450, chunks=15
DEBUG: Assistant message persisted - messageId=msg124
```

## Observações

- Todos os logs mantêm consistência no formato
- IDs (chatId, messageId, fileName) são sempre incluídos para correlação
- Tempos de execução em milissegundos para operações críticas
- Níveis de log apropriados para cada situação
- Facilita troubleshooting e análise de performance

