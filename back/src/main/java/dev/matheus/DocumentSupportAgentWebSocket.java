package dev.matheus;

import dev.langchain4j.guardrail.InputGuardrailException;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;

@WebSocket(path = "/document-support-agent")
public class DocumentSupportAgentWebSocket {

    private final DocumentSupportAgent documentSupportAgent;

    public DocumentSupportAgentWebSocket(DocumentSupportAgent documentSupportAgent) {
        this.documentSupportAgent = documentSupportAgent;
    }

    @OnOpen
    public String onOpen() {
        return "Bem vindo ao DocIntel! Como posso te ajudar?";
    }

    @OnTextMessage
    public Multi<String> onTextMessage(String message) {
        try {
            return documentSupportAgent.chat(message);
        } catch (InputGuardrailException e) {
            Log.errorf(e, "Error calling the LLM: %s", e.getMessage());
            return Multi.createFrom().item("Sorry, I am unable to process your request at the moment. It's not something I'm allowed to do.");
        } catch (Exception e) {
            Log.errorf(e, "Error calling the LLM: %s", e.getMessage());
            return Multi.createFrom().item("I ran into some problems. Please try again.");
        }
    }
}