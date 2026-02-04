package dev.matheus.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.matheus.dto.ChatListResponse;
import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.dto.ChatResponse;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.entity.Chat;
import dev.matheus.entity.ChatMessage;
import dev.matheus.entity.DocumentFile;
import dev.matheus.entity.ProcessingStatus;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.event.DocumentCreatedEvent;
import dev.matheus.repository.ChatMessageRepository;
import dev.matheus.repository.ChatRepository;
import dev.matheus.repository.DocumentFileRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class ChatService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);
    private static final int MAX_CONVERSATION_HISTORY = 10; // Keep last 10 messages (5 exchanges)

    @Inject
    ChatRepository chatRepository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    DocumentFileRepository documentFileRepository;

    @Inject
    RetrievalInfoService retrievalInfoService;

    @Inject
    jakarta.persistence.EntityManager entityManager;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    DocumentIngestionService documentIngestionService;

    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;

    @Inject
    AsyncDocumentProcessingService asyncProcessingService;

    @Inject
    Event<DocumentCreatedEvent> documentCreatedEvent;


    @Transactional
    public ChatResponse createChat(byte[] fileData, String fileName, String fileType) throws IOException {
        LOG.infof("Creating chat with document: fileName=%s, fileType=%s, size=%d bytes",
                fileName, fileType, fileData != null ? fileData.length : 0);

        if (fileData == null) {
            LOG.error("Attempted to create chat with null file data");
            throw new IllegalArgumentException("File data cannot be null");
        }

        Chat chat = new Chat();
        // Gerar título baseado no nome do arquivo (remover extensão)
        chat.title = generateTitleFromFileName(fileName);
        LOG.debugf("Generated chat title: %s", chat.title);

        DocumentFile documentFile = new DocumentFile();
        documentFile.chat = chat;
        documentFile.fileData = fileData;
        documentFile.fileName = fileName;
        documentFile.fileType = fileType;
        documentFile.fileSize = (long) fileData.length;
        documentFile.processingStatus = ProcessingStatus.PENDING; // Initialize with PENDING status

        chat.documentFile = documentFile;

        chatRepository.persist(chat);
        LOG.infof("Chat created successfully: chatId=%s, documentId=%s, title=%s",
                chat.id, documentFile.id, chat.title);

        // Fire CDI event that will be handled AFTER transaction commits
        // The listener observes this event with TransactionPhase.AFTER_SUCCESS
        // This ensures the document is visible to async processing threads
        LOG.infof("Firing document created event: docId=%s, fileName=%s", documentFile.id, fileName);
        documentCreatedEvent.fire(new DocumentCreatedEvent(documentFile.id, fileName));

        return mapToChatResponse(chat);
    }

    private String generateTitleFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "Novo Chat";
        }
        // Remover extensão do arquivo
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }


    public Chat getChat(String chatId) {
        LOG.debugf("Fetching chat from database: chatId=%s", chatId);
        Chat chat = chatRepository.findByIdOptional(chatId)
                .orElseThrow(() -> {
                    LOG.warnf("Chat not found: chatId=%s", chatId);
                    return new NotFoundException("Chat not found");
                });
        LOG.debugf("Chat found: chatId=%s", chatId);
        return chat;
    }

    public List<ChatListResponse> listChats() {
        LOG.debug("Fetching all chats (id and title only) from database");
        List<ChatListResponse> chats = chatRepository.findAllIdAndTitle();
        LOG.debugf("Found %d chats in database", chats.size());
        return chats;
    }

    @Transactional
    public void deleteChat(String chatId) {
        LOG.infof("Deleting chat from database: chatId=%s", chatId);
        Chat chat = chatRepository.findByIdOptional(chatId)
                .orElseThrow(() -> {
                    LOG.warnf("Chat not found for deletion: chatId=%s", chatId);
                    return new NotFoundException("Chat not found");
                });

        String filename = chat.documentFile.fileName;
        chatRepository.delete(chat);

        try {
            // delete all embeddings related to this chat
            embeddingStore.removeAll(new IsEqualTo("FILE_NAME", filename));
        } catch (Exception ex) {
            Log.error("Could not execute REMOVEALL", ex);
        }

        LOG.infof("All embeddings related to filename=%s have been deleted", filename);
    }

    @Transactional
    public void deleteChatMessages(String chatId) {
        LOG.infof("Deleting all messages for chat: chatId=%s", chatId);
        long deletedCount = chatMessageRepository.deleteMessages(chatId);
        LOG.infof("Deleted %d messages for chatId=%s", deletedCount, chatId);
    }

    public ChatMessageResponse getLastUserMessage(String chatId) {
        LOG.debugf("Fetching last user message for chat: chatId=%s", chatId);
        return chatMessageRepository.findLastUserMessageByChatId(chatId)
                .map(this::mapToChatMessageResponse)
                .orElseThrow(() -> {
                    LOG.warnf("No user messages found for chat: chatId=%s", chatId);
                    return new NotFoundException("No user messages found for this chat");
                });
    }

    public void saveEmptyRetrievalInfoIfNeeded(String chatId, String userQuestion) {
        LOG.debugf("Checking if empty RetrievalInfo is needed: chatId=%s", chatId);
        var lastMessage = chatMessageRepository.findLastUserMessageByChatId(chatId)
                .orElseThrow(() -> {
                    LOG.warnf("No user messages found for chat: chatId=%s", chatId);
                    return new NotFoundException("No user messages found for this chat");
                });

        if (lastMessage.retrievalInfo == null) {
            Log.infof("No RetrievalInfo found for last user message. Saving empty RetrievalInfo - messageId=%s",
                    lastMessage.id);

            RetrievalInfo info = new RetrievalInfo();
            info.userQuestion = userQuestion;

            lastMessage.retrievalInfo = info;
            chatMessageRepository.persist(lastMessage);
            Log.infof("Empty RetrievalInfo saved successfully - messageId=%s", lastMessage.id);
        } else {
            LOG.debugf("RetrievalInfo already exists for messageId=%s, skipping", lastMessage.id);
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ChatMessageResponse addUserMessageAndCommit(String chatId, String content) {
        ChatMessageResponse response = addMessage(chatId, "user", content);
        entityManager.flush(); // Force flush to database
        LOG.debugf("User message saved and committed in separate transaction: chatId=%s, messageId=%s",
                chatId, response.id());
        return response;
    }

    @Transactional
    public ChatMessageResponse addAssistantMessage(String chatId, String content) {
        return addMessage(chatId, "assistant", content);
    }

    @Transactional
    public ChatMessageResponse addErrorMessage(String chatId, String errorMessage) {
        return addMessage(chatId, "system", errorMessage);
    }

    private ChatMessageResponse addMessage(String chatId, String role, String content) {
        LOG.infof("Adding message to chat: chatId=%s, role=%s, contentLength=%d",
                chatId, role, content != null ? content.length() : 0);

        try {
            Chat chat = chatRepository.findByIdOptional(chatId)
                    .orElseThrow(() -> {
                        LOG.warnf("Chat not found when adding message: chatId=%s", chatId);
                        return new NotFoundException("Chat not found");
                    });

            ChatMessage message = new ChatMessage();
            message.chat = chat;
            message.role = role;
            message.content = content;

            chatMessageRepository.persist(message);
            LOG.infof("Message added successfully: chatId=%s, messageId=%s, role=%s",
                    chatId, message.id, role);

            return mapToChatMessageResponse(message);
        } catch (Exception e) {
            Log.errorf(e, "Failed to save {} message: chatId=%s", role, chatId);
            throw e;
        }
    }

    public List<ChatMessageResponse> getChatMessages(String chatId) {
        LOG.debugf("Fetching messages for chat: chatId=%s", chatId);
        List<ChatMessage> messages = chatMessageRepository.findByChatId(chatId);
        LOG.debugf("Found %d messages for chatId=%s", messages.size(), chatId);
        return messages.stream()
                .map(this::mapToChatMessageResponse)
                .toList();
    }

    @Tool(value = "Recupera um intervalo de mensagens do tipo USUÁRIO do histórico do chat. Exemplo: getRecentUserMessages(chatId, 0, 5) retorna as primeiras 5 mensagens do tipo USUÁRIO")
    public List<String> getRecentUserMessages(
            @P("ID do chat") String chatId,
            @P("Índice inicial (baseado em 0)") int startIdx,
            @P("Índice final (exclusivo)") int endIdx) {

        LOG.debugf("Fetching recent USER conversation history for chat: chatId=%s", chatId);

        List<ChatMessage> allMessages = chatMessageRepository.findByChatId(chatId);

        // Filter out system messages and welcome messages and last message (since is the current one seeing by AI)
        List<ChatMessage> relevantMessages = allMessages.stream()
                .filter(msg -> !msg.role.equals("system")) // Exclude system messages
                .filter(msg -> !isWelcomeMessage(msg.content)) // Exclude welcome messages
                .limit(Math.max(0, allMessages.size() - 1)) // Exclude last message
                .toList();

        // Bounds checking to prevent IndexOutOfBoundsException
        int safeStartIdx = Math.max(0, Math.min(startIdx, relevantMessages.size()));
        int safeEndIdx = Math.max(safeStartIdx, Math.min(endIdx, relevantMessages.size()));

        List<String> result = relevantMessages.subList(safeStartIdx, safeEndIdx).stream()
                .map(msg -> msg.content)
                .toList();

        LOG.debugf("Retrieving %s USER messages from chatId=%s", result.size(), chatId);
        return result;
    }

    @Tool(value = "Recupera um intervalo de mensagens do tipo ASSISTENTE do histórico do chat. Exemplo: getRecentAiResponses(chatId, 0, 5) retorna as primeiras 5 mensagens do tipo ASSISTENTE")
    public List<String> getRecentAiResponses(
            @P("ID do chat") String chatId,
            @P("Índice inicial (baseado em 0)") int startIdx,
            @P("Índice final (exclusivo)") int endIdx) {

        LOG.debugf("Fetching recent AI conversation history for chat: chatId=%s", chatId);

        List<ChatMessage> allMessages = chatMessageRepository.findByChatId(chatId);

        // Filter out user messages and welcome messages
        List<ChatMessage> relevantMessages = allMessages.stream()
                .filter(msg -> !msg.role.equals("user")) // Exclude system messages
                .filter(msg -> !isWelcomeMessage(msg.content)) // Exclude welcome messages
                .toList();

        List<String> result = relevantMessages.subList(startIdx, endIdx).stream()
                .map(msg -> msg.content)
                .toList();

        LOG.debugf("Retrieving %s AI messages from chatId=%s", result.size(), chatId);
        return result;
    }

    @Tool(value = "Retorna o número de mensagens do assistente")
    public long getNumberOfAssistantMessages(String chatId) {
        return chatMessageRepository.countNumberOfAssistantMessages(chatId);
    }

    @Tool(value = "Retorna o número de mensagens do usuário")
    public long getNumberOfUserMessages(String chatId) {
        return chatMessageRepository.countNumberOfUserMessages(chatId);
    }

    /**
     * Check if a message is a welcome message
     */
    private boolean isWelcomeMessage(String content) {
        return content != null && (
                content.contains("Bem vindo ao DocIntel") ||
                        content.contains("É muito bom ter você de volta") ||
                        content.contains("Welcome to DocIntel") ||
                        content.contains("É necessário fazer upload")
        );
    }

    @Tool("retorna metadata do documento")
    @Transactional
    public DocumentFileResponse getDocument(String chatId) {
        return documentFileRepository.findByChatId(chatId)
                .map(this::mapToDocumentFileResponse)
                .orElseThrow(() -> new NotFoundException("Document not found for this chat"));
    }

    public DocumentFile getDocumentEntity(String chatId) {
        return documentFileRepository.findByChatId(chatId)
                .orElseThrow(() -> new NotFoundException("Document not found for this chat"));
    }
    
    public dev.matheus.dto.DocumentStatusDTO getDocumentStatus(String chatId) {
        return documentFileRepository.findStatusByChatId(chatId)
                .orElseThrow(() -> new NotFoundException("Document not found for this chat"));
    }

    private ChatResponse mapToChatResponse(Chat chat) {
        return new ChatResponse(
                chat.id,
                chat.title,
                chat.createdAt,
                chat.updatedAt,
                chat.documentFile != null
        );
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.id,
                message.role,
                message.content,
                message.createdAt
        );
    }

    private DocumentFileResponse mapToDocumentFileResponse(DocumentFile file) {
        return new DocumentFileResponse(
                file.id,
                file.fileName,
                file.fileType,
                file.fileSize,
                file.uploadedAt
        );
    }
}