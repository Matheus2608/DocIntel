package dev.matheus.service;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatSessionRegistry {

    private static final Logger LOG = Logger.getLogger(ChatSessionRegistry.class);

    private final Map<String, WebSocketConnection> sessions = new ConcurrentHashMap<>();

    public void register(String chatId, WebSocketConnection connection) {
        sessions.put(chatId, connection);
    }

    public void unregister(String chatId) {
        sessions.remove(chatId);
    }

    public void sendToolCallEvent(String chatId, String toolName, String status) {
        sendToolCallEvent(chatId, toolName, status, null);
    }

    public void sendToolCallEvent(String chatId, String toolName, String status, String argumentsJson) {
        WebSocketConnection connection = sessions.get(chatId);
        if (connection == null) {
            return;
        }

        StringBuilder json = new StringBuilder(128);
        json.append("{\"type\":\"tool_call\",\"tool\":\"")
                .append(escapeJson(toolName))
                .append("\",\"status\":\"")
                .append(escapeJson(status))
                .append("\"");
        if (argumentsJson != null && !argumentsJson.isBlank()) {
            json.append(",\"arguments\":").append(argumentsJson);
        }
        json.append("}");

        connection.sendText(json.toString()).subscribe().with(v -> {}, e ->
                LOG.warnf("Failed to send tool_call event for chatId=%s: %s", chatId, e.getMessage()));
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
