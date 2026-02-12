package dev.matheus;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.matheus.ai.DocumentSupportAgent;
import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.entity.Chat;
import dev.matheus.entity.DocumentFile;
import dev.matheus.service.ChatService;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;

@WebSocket(path = "/document-support-agent/{chatId}")
public class DocumentSupportAgentWebSocket {

    private static final String WELCOME_MESSAGE = "Bem vindo ao DocIntel! Conectado ao chat '%s'. Como posso te ajudar?";
    private static final String WELCOME_BACK_MESSAGE = "É muito bom ter você de volta ao DocIntel! Como posso te ajudar?";

    private final DocumentSupportAgent documentSupportAgent;

    @Inject
    private ChatService chatService;

    public DocumentSupportAgentWebSocket(DocumentSupportAgent documentSupportAgent) {
        this.documentSupportAgent = documentSupportAgent;
    }

    @OnOpen
    @RunOnVirtualThread
    @Transactional
    public String onOpen(WebSocketConnection connection) {
        return "";
//        String chatId = connection.pathParam("chatId");
//        Log.infof("WebSocket connection opened - chatId=%s, connectionId=%s", chatId, connection.id());
//
//        Chat chat = chatService.getChat(chatId);
//
//        Log.infof("Chat loaded for WebSocket session: chatId=%s, title=%s, hasDocument=%s",
//                chatId, chat.title, chat.documentFile != null);
//
//        if (chat.documentFile != null) {
//            DocumentFile doc = chat.documentFile;
//            Log.debugf("Document available in session: fileName=%s, size=%d bytes",
//                    doc.fileName, doc.fileSize);
//            return getOnOpenMessage(chatId, chat.title);
//
//        } else {
//            Log.warnf("No document found for chat: chatId=%s, title=%s", chatId, chat.title);
//            return String.format("É necessário fazer upload de um documento para iniciar o chat '%s'.", chat.title);
//        }
    }

    private String getOnOpenMessage(String chatId, String title) {
        List<ChatMessageResponse> messages = chatService.getChatMessages(chatId);
        if (messages.isEmpty()) {
            return String.format(WELCOME_MESSAGE, title);
        }

        if (Objects.equals(messages.getLast().content(), WELCOME_BACK_MESSAGE)) {
            return null;
        } else {
            return WELCOME_BACK_MESSAGE;
        }
    }

    @OnTextMessage
    @RunOnVirtualThread
    public Multi<String> onTextMessage(String message, WebSocketConnection connection) {
        String chatId = connection.pathParam("chatId");
        Log.infof("Received message on WebSocket: chatId=%s, connectionId=%s, messageLength=%d",
                chatId, connection.id(), message != null ? message.length() : 0);
        Log.debugf("Message content: %s", message);

        ChatMessageResponse userMessage = null;
        try {
            // Save user message in a separate transaction that will be committed immediately
            userMessage = chatService.addUserMessageAndCommit(chatId, message);
            final String userMessageId = userMessage.id();
            Log.debugf("User message committed to database before AI call: chatId=%s, messageId=%s",
                    chatId, userMessageId);

            Multi<String> response = documentSupportAgent.chat(message, chatId);

            return response
                    .collect().asList()
                    .onItem().transformToMulti(chunks -> {
                        String fullResponse = String.join("", chunks);
                        Log.debugf("AI response generated: chatId=%s, responseLength=%d",
                                chatId, fullResponse.length());
                        var chatResponse = chatService.addAssistantMessage(chatId, fullResponse);
                        chatService.saveEmptyRetrievalInfoIfNeeded(chatId, message);

                        // Envia chunks durante o streaming + mensagem final com ID e conteúdo completo
                        return Multi.createFrom().iterable(chunks)
                                .onCompletion().continueWith(
                                    String.format("{\"messageId\":\"%s\",\"content\":\"%s\",\"type\":\"complete\"}",
                                        userMessageId,
                                        fullResponse.replace("\"", "\\\"").replace("\n", "\\n"))
                                );
                    });

        } catch (InputGuardrailException e) {
            Log.errorf(e, "Input guardrail violation - chatId=%s: %s", chatId, e.getMessage());
            String errorMessage = "Sorry, I am unable to process your request at the moment. It's not something I'm allowed to do.";
            var errorResponse = chatService.addErrorMessage(chatId, "Input blocked by guardrails");
            String messageId = userMessage != null ? userMessage.id() : errorResponse.id();
            return Multi.createFrom().item(errorMessage)
                    .onCompletion().continueWith(
                        String.format("{\"messageId\":\"%s\",\"content\":\"%s\",\"type\":\"error\"}",
                            messageId,
                            errorMessage.replace("\"", "\\\""))
                    );
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error in WebSocket - chatId=%s: %s", chatId, e.getMessage());
            String errorMessage = "I ran into some problems. Please try again.";
            var errorResponse = chatService.addErrorMessage(chatId, "Error: " + e.getMessage());
            String messageId = userMessage != null ? userMessage.id() : errorResponse.id();
            return Multi.createFrom().item(errorMessage)
                    .onCompletion().continueWith(
                        String.format("{\"messageId\":\"%s\",\"content\":\"%s\",\"type\":\"error\"}",
                            messageId,
                            errorMessage.replace("\"", "\\\""))
                    );
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        String chatId = connection.pathParam("chatId");
        Log.infof("WebSocket connection closed - chatId=%s, connectionId=%s", chatId, connection.id());
    }

}