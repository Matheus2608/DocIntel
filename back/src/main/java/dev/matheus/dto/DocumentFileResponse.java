package dev.matheus.dto;

import java.time.LocalDateTime;

public record DocumentFileResponse(
    String id,
    String fileName,
    String fileType,
    Long fileSize,
    LocalDateTime uploadedAt
) {}

