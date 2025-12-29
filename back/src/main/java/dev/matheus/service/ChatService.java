package dev.matheus.service;

import dev.langchain4j.agent.tool.Tool;
import dev.matheus.dto.ChatListResponse;
import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.dto.ChatResponse;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.entity.Chat;
import dev.matheus.entity.ChatMessage;
import dev.matheus.entity.DocumentFile;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.rag.RagIngestion;
import dev.matheus.repository.ChatMessageRepository;
import dev.matheus.repository.ChatRepository;
import dev.matheus.repository.DocumentFileRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ChatService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);

    @Inject
    ChatRepository chatRepository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    DocumentFileRepository documentFileRepository;

    @Inject
    RagIngestion ragIngestion;

    @Inject
    jakarta.persistence.EntityManager entityManager;

    @Transactional
    public ChatResponse createChat(byte[] fileData, String fileName, String fileType) {
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

        chat.documentFile = documentFile;

        chatRepository.persist(chat);
        LOG.infof("Chat created successfully: chatId=%s, documentId=%s, title=%s",
                chat.id, documentFile.id, chat.title);

        Log.infof("Starting RAG ingestion for file=%s", fileName);
        ragIngestion.ingestionOfHypotheticalQuestions(fileData);

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
        boolean deleted = chatRepository.deleteById(chatId);
        if (deleted) {
            LOG.infof("Chat deleted successfully: chatId=%s", chatId);
        } else {
            LOG.warnf("Chat not found for deletion: chatId=%s", chatId);
        }
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
        var lastMessage = chatMessageRepository.findLastUserMessageByChatId(chatId)
                .orElseThrow(() -> {
                    LOG.warnf("No user messages found for chat: chatId=%s", chatId);
                    return new NotFoundException("No user messages found for this chat");
                });

        if (lastMessage.retrievalInfo == null) {
            Log.infof("No RetrievalInfo found for last user message ID: %s. Saving empty RetrievalInfo.",
                    lastMessage.id);

            RetrievalInfo info = new RetrievalInfo();
            info.userQuestion = userQuestion;

            lastMessage.retrievalInfo = info;
            chatMessageRepository.persist(lastMessage);
            Log.infof("Empty RetrievalInfo saved for ChatMessage ID: %s", lastMessage.id);
        }
    }

    @Transactional
    public ChatMessageResponse addUserMessage(String chatId, String content) {
        ChatMessageResponse response = addMessage(chatId, "user", content);
        entityManager.flush(); // Force immediate commit to database
        LOG.debugf("User message flushed to database: chatId=%s, messageId=%s", chatId, response.id());
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


    @Tool("get-document-info")
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

