package dev.matheus.service;

import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.dto.ChatResponse;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.entity.Chat;
import dev.matheus.entity.ChatMessage;
import dev.matheus.entity.DocumentFile;
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

//    public List<ChatResponse> listChats() {
//        LOG.debug("Fetching all chats from database");
//        List<Chat> chats = chatRepository.listAll();
//        LOG.debugf("Found %d chats in database", chats.size());
//        return chats.stream()
//                .map(this::mapToChatResponse)
//                .toList();
//    }

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

    public ChatMessageResponse addUserMessage(String chatId, String content) {
        return addMessage(chatId, "user", content);
    }

    public ChatMessageResponse addAssistantMessage(String chatId, String content) {
        return addMessage(chatId, "assistant", content);
    }

    public ChatMessageResponse addErrorMessage(String chatId, String errorMessage) {
        return addMessage(chatId, "system", errorMessage);
    }
    @Transactional
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

//    @Transactional
//    public DocumentFileResponse uploadDocument(String chatId, String fileName, String fileType,
//                                                Long fileSize, byte[] fileData) {
//        LOG.infof("Uploading document to chat: chatId=%s, fileName=%s, fileType=%s, size=%d bytes",
//                  chatId, fileName, fileType, fileSize);
//
//        Chat chat = chatRepository.findByIdOptional(chatId)
//                .orElseThrow(() -> {
//                    LOG.warnf("Chat not found when uploading document: chatId=%s", chatId);
//                    return new NotFoundException("Chat not found");
//                });
//
//        // Remove documento anterior se existir
//        documentFileRepository.findByChatId(chatId)
//                .ifPresent(oldDoc -> {
//                    LOG.infof("Removing old document: chatId=%s, oldDocumentId=%s", chatId, oldDoc.id);
//                    documentFileRepository.delete(oldDoc);
//                });
//
//        DocumentFile documentFile = new DocumentFile();
//        documentFile.chat = chat;
//        documentFile.fileName = fileName;
//        documentFile.fileType = fileType;
//        documentFile.fileSize = fileSize;
//        documentFile.fileData = fileData;
//
//        documentFileRepository.persist(documentFile);
//        LOG.infof("Document uploaded successfully: chatId=%s, documentId=%s", chatId, documentFile.id);
//
//        return mapToDocumentFileResponse(documentFile);
//    }

    public DocumentFileResponse getDocument(String chatId) {
        LOG.debugf("Fetching document info for chat: chatId=%s", chatId);
        return documentFileRepository.findByChatId(chatId)
                .map(doc -> {
                    LOG.debugf("Document found: chatId=%s, documentId=%s, fileName=%s",
                               chatId, doc.id, doc.fileName);
                    return mapToDocumentFileResponse(doc);
                })
                .orElseThrow(() -> {
                    LOG.warnf("Document not found for chat: chatId=%s", chatId);
                    return new NotFoundException("Document not found for this chat");
                });
    }

    public DocumentFile getDocumentEntity(String chatId) {
        LOG.debugf("Fetching document entity for chat: chatId=%s", chatId);
        return documentFileRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    LOG.warnf("Document entity not found for chat: chatId=%s", chatId);
                    return new NotFoundException("Document not found for this chat");
                });
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

