# Configuração de Logging e Observabilidade

Este documento descreve a configuração de logging implementada no DocIntel para debugging e observabilidade.

## Níveis de Log

A aplicação utiliza os seguintes níveis de log:

- **TRACE**: Informações muito detalhadas (ex: parâmetros de bind do Hibernate)
- **DEBUG**: Informações de debugging (ex: consultas SQL, detalhes de requisições)
- **INFO**: Informações gerais da aplicação (ex: criação de recursos, operações principais)
- **WARN**: Avisos (ex: recursos não encontrados, validações que falharam)
- **ERROR**: Erros que precisam atenção (ex: exceções, falhas de processamento)

## Configuração (application.properties)

```properties
# Log geral da aplicação
quarkus.log.level=INFO
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n

# Log da aplicação (dev.matheus)
quarkus.log.category."dev.matheus".level=DEBUG

# HTTP Access Log (requisições e respostas)
quarkus.http.access-log.enabled=true
quarkus.http.access-log.pattern=%h %l %u %t "%r" %s %b "%{i,Referer}" "%{i,User-Agent}"

# Database (SQL queries e bind parameters)
quarkus.log.category."org.hibernate.SQL".level=DEBUG
quarkus.log.category."org.hibernate.type.descriptor.sql.BasicBinder".level=TRACE

# RESTEasy Reactive
quarkus.log.category."org.jboss.resteasy.reactive".level=DEBUG

# WebSocket
quarkus.log.category."io.quarkus.websockets".level=DEBUG

# LangChain4j
quarkus.langchain4j.ollama.chat-model.log-requests=true
quarkus.langchain4j.ollama.chat-model.log-responses=true
quarkus.log.category."io.quarkiverse.langchain4j.pgvector".level=DEBUG
```

## Componentes com Logging

### 1. ChatResource (REST API)

**Logs implementados:**
- INFO: Criação de chats, adição de mensagens, deleções, downloads
- DEBUG: Listagem de recursos, busca por ID
- WARN: Validações de arquivo que falharam
- ERROR: Erros ao processar arquivos

**Exemplo de logs:**
```
INFO  [dev.matheus.resource.ChatResource] Creating new chat with file upload: fileName=document.pdf, contentType=application/pdf
DEBUG [dev.matheus.resource.ChatResource] File read successfully: size=1024576 bytes
INFO  [dev.matheus.resource.ChatResource] Chat created successfully: chatId=550e8400-e29b-41d4-a716-446655440000
```

### 2. ChatService (Lógica de Negócio)

**Logs implementados:**
- INFO: Operações de persistência (create, delete, upload)
- DEBUG: Consultas ao banco, contagem de recursos
- WARN: Recursos não encontrados
- ERROR: Validações que falharam

**Exemplo de logs:**
```
INFO  [dev.matheus.service.ChatService] Creating chat with document: fileName=doc.pdf, fileType=application/pdf, size=1024576 bytes
INFO  [dev.matheus.service.ChatService] Chat created successfully: chatId=550e8400-e29b-41d4-a716-446655440000, documentId=660e8400-e29b-41d4-a716-446655440001
```

### 3. DocumentSupportAgentWebSocket

**Logs implementados:**
- INFO: Conexões abertas, mensagens recebidas
- DEBUG: Conteúdo das mensagens, chamadas ao agent
- ERROR: Erros no LLM, guardrails violations

**Exemplo de logs:**
```
INFO  [dev.matheus.DocumentSupportAgentWebSocket] WebSocket connection opened - New client connected
INFO  [dev.matheus.DocumentSupportAgentWebSocket] Received message from client: length=150 chars
DEBUG [dev.matheus.DocumentSupportAgentWebSocket] Successfully called documentSupportAgent.chat()
```

### 4. CorsFilter

**Logs implementados:**
- DEBUG: Requisições CORS, preflight requests
- TRACE: Adição de headers CORS nas respostas

**Exemplo de logs:**
```
DEBUG [dev.matheus.CorsFilter] CORS filter - request: method=POST, path=/api/chats, origin=http://localhost:5173
DEBUG [dev.matheus.CorsFilter] Handling preflight OPTIONS request for path=/api/chats
```

## Padrões de Log

### Formato das Mensagens

Usamos o seguinte padrão para mensagens de log:

```java
// INFO - Operações principais com contexto
LOG.infof("Creating new chat with file upload: fileName=%s, contentType=%s", fileName, contentType);

// DEBUG - Detalhes técnicos
LOG.debugf("File read successfully: size=%d bytes", fileData.length);

// WARN - Situações anormais não críticas
LOG.warnf("Invalid file type rejected: contentType=%s", contentType);

// ERROR - Erros com stack trace
LOG.errorf(e, "Failed to read uploaded file: %s", fileName);
```

### Informações Contextuais

Sempre incluímos contexto relevante nos logs:

- **IDs de recursos**: `chatId`, `messageId`, `documentId`
- **Tamanhos**: `size` (em bytes), `contentLength` (em caracteres)
- **Tipos**: `contentType`, `fileType`, `role`
- **Nomes**: `fileName`, `path`, `method`

## Ajustando Níveis de Log em Produção

Para produção, recomenda-se ajustar os seguintes níveis:

```properties
# Reduzir verbosidade geral
quarkus.log.level=WARN

# Manter logs da aplicação em INFO
quarkus.log.category."dev.matheus".level=INFO

# Desabilitar logs SQL detalhados
quarkus.log.category."org.hibernate.SQL".level=WARN
quarkus.log.category."org.hibernate.type.descriptor.sql.BasicBinder".level=WARN

# Desabilitar logs do RESTEasy
quarkus.log.category."org.jboss.resteasy.reactive".level=INFO
```

## Monitoramento e Observabilidade

### Métricas Importantes

Com os logs implementados, você pode monitorar:

1. **Taxa de requisições**: Access logs mostram todas as requisições HTTP
2. **Erros**: Logs ERROR indicam problemas que precisam atenção
3. **Performance**: Tempo entre logs INFO pode indicar operações lentas
4. **Uso**: Quantidade de chats criados, mensagens enviadas, documentos processados

### Ferramentas Recomendadas

Para ambiente de produção, considere integrar com:

- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Grafana + Loki**
- **Prometheus** (para métricas)
- **Jaeger** (para tracing distribuído)

### Formato JSON para Produção

Para facilitar parsing por ferramentas de log, ative o formato JSON:

```properties
quarkus.log.console.json=true
```

## Troubleshooting

### Problema: Logs duplicados

**Solução**: Verifique se não há múltiplos handlers configurados

### Problema: Logs não aparecem

**Solução**: Verifique o nível de log está adequado para a categoria

### Problema: Performance degradada com logging

**Solução**: Ajuste níveis para WARN/ERROR em produção, especialmente para Hibernate

## Exemplos de Uso

### Debugging de upload de arquivo

```bash
# Procurar logs de uploads
grep "Creating new chat with file upload" application.log

# Verificar erros de processamento
grep "Failed to read uploaded file" application.log
```

### Monitoring de performance

```bash
# Ver todas operações de criação de chat
grep "Chat created successfully" application.log

# Contar mensagens processadas
grep "Message added successfully" application.log | wc -l
```

### Análise de erros

```bash
# Ver todos os erros
grep "ERROR" application.log

# Erros de WebSocket
grep "ERROR.*DocumentSupportAgentWebSocket" application.log
```

