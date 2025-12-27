package dev.matheus.dto;

import java.time.LocalDateTime;

public record ChatResponse(
    String id,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean hasDocument
) {}

