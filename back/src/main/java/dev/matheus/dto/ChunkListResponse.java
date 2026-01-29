package dev.matheus.dto;

import java.util.List;

/**
 * Response DTO for paginated chunk list.
 */
public record ChunkListResponse(
        List<ChunkResponse> chunks,
        Long totalCount,
        Integer page,
        Integer size,
        Boolean hasMore
) {}
