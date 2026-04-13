package dev.matheus.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    String id,
    String role,
    String content,
    LocalDateTime createdAt,
    int stepCount
) {
    public ChatMessageResponse(String id, String role, String content, LocalDateTime createdAt) {
        this(id, role, content, createdAt, 0);
    }
}

