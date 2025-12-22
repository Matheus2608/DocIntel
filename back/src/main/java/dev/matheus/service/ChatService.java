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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class ChatService {

    @Inject
    ChatRepository chatRepository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    DocumentFileRepository documentFileRepository;

    @Transactional
    public ChatResponse createChat() {
        Chat chat = new Chat();
        chatRepository.persist(chat);
        return mapToChatResponse(chat);
    }

    public ChatResponse getChat(String chatId) {
        Chat chat = chatRepository.findByIdOptional(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));
        return mapToChatResponse(chat);
    }

    public List<ChatResponse> listChats() {
        return chatRepository.listAll().stream()
                .map(this::mapToChatResponse)
                .toList();
    }

    @Transactional
    public void deleteChat(String chatId) {
        chatRepository.deleteById(chatId);
    }

    @Transactional
    public ChatMessageResponse addMessage(String chatId, String role, String content) {
        Chat chat = chatRepository.findByIdOptional(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        ChatMessage message = new ChatMessage();
        message.chat = chat;
        message.role = role;
        message.content = content;

        chatMessageRepository.persist(message);
        return mapToChatMessageResponse(message);
    }

    public List<ChatMessageResponse> getChatMessages(String chatId) {
        return chatMessageRepository.findByChatId(chatId).stream()
                .map(this::mapToChatMessageResponse)
                .toList();
    }

    @Transactional
    public DocumentFileResponse uploadDocument(String chatId, String fileName, String fileType,
                                                Long fileSize, byte[] fileData) {
        Chat chat = chatRepository.findByIdOptional(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found"));

        // Remove documento anterior se existir
        documentFileRepository.findByChatId(chatId)
                .ifPresent(documentFileRepository::delete);

        DocumentFile documentFile = new DocumentFile();
        documentFile.chat = chat;
        documentFile.fileName = fileName;
        documentFile.fileType = fileType;
        documentFile.fileSize = fileSize;
        documentFile.fileData = fileData;

        documentFileRepository.persist(documentFile);
        return mapToDocumentFileResponse(documentFile);
    }

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

