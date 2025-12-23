package dev.matheus;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.matheus.entity.Chat;
import dev.matheus.entity.DocumentFile;
import dev.matheus.service.ChatService;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@WebSocket(path = "/document-support-agent/{chatId}")
public class DocumentSupportAgentWebSocket {

    private final DocumentSupportAgent documentSupportAgent;

    @Inject
    private ChatService chatService;

    public DocumentSupportAgentWebSocket(DocumentSupportAgent documentSupportAgent) {
        this.documentSupportAgent = documentSupportAgent;
    }

    @OnOpen
    public Uni<String> onOpen(WebSocketConnection connection, String chatId) {
        Log.infof("WebSocket connection opened - chatId=%s, connectionId=%s", chatId, connection.id());

        return Uni.createFrom().item(() -> {
            // Verificar se o chat existe

            Chat chat = chatService.getChat(chatId);

            Log.infof("Chat loaded for WebSocket session: chatId=%s, title=%s, hasDocument=%s",
                      chatId, chat.title, chat.documentFile != null);

            // Carregar contexto do documento se existir
            if (chat.documentFile != null) {
                DocumentFile doc = chat.documentFile;
                Log.debugf("Document available in session: fileName=%s, size=%d bytes",
                           doc.fileName, doc.fileSize);
                return String.format("Bem vindo ao DocIntel! Conectado ao chat '%s'. Como posso te ajudar?",
                        chat.title);
            } else {
                Log.warnf("No document found for chat: chatId=%s, title=%s", chatId, chat.title);
                return String.format("É necessário fazer upload de um documento para iniciar o chat '%s'.", chat.title);
            }
        });
    }

    @OnTextMessage
    public Multi<String> onTextMessage(String message, String chatId, WebSocketConnection connection) {
        Log.infof("Received message on WebSocket: chatId=%s, connectionId=%s, messageLength=%d",
                  chatId, connection.id(), message != null ? message.length() : 0);
        Log.debugf("Message content: %s", message);

        try {
            // Salvar mensagem do usuário
            chatService.addUserMessage(chatId, message);

            // Chamar o agente e obter resposta
            Multi<String> response = documentSupportAgent.chat(message, chatId);

            // Coletar a resposta completa e salvar
            return response
                    .collect().asList()
                    .onItem().transformToMulti(chunks -> {
                        String fullResponse = String.join("", chunks);
                        Log.debugf("AI response generated: chatId=%s, responseLength=%d",
                                   chatId, fullResponse.length());

                        // Salvar resposta do assistente
                        chatService.addAssistantMessage(chatId, fullResponse);

                        return Multi.createFrom().iterable(chunks);
                    });

        } catch (InputGuardrailException e) {
            Log.errorf(e, "Input guardrail violation - chatId=%s: %s", chatId, e.getMessage());
            chatService.addErrorMessage(chatId, "Input blocked by guardrails");
            return Multi.createFrom().item("Sorry, I am unable to process your request at the moment. It's not something I'm allowed to do.");
        } catch (Exception e) {
            Log.errorf(e, "Unexpected error in WebSocket - chatId=%s: %s", chatId, e.getMessage());
            chatService.addErrorMessage(chatId, "Error: " + e.getMessage());
            return Multi.createFrom().item("I ran into some problems. Please try again.");
        }
    }

    @OnClose
    public void onClose(String chatId, WebSocketConnection connection) {
        Log.infof("WebSocket connection closed - chatId=%s, connectionId=%s", chatId, connection.id());
    }

}