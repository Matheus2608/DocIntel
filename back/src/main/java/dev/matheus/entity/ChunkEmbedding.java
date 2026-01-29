package dev.matheus.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Links document chunks to their vector embeddings in pgvector store.
 * Supports multiple embedding types per chunk (content, hypothetical questions).
 */
@Entity
@Table(name = "chunk_embedding")
public class ChunkEmbedding extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    public DocumentChunk chunk;

    @Column(name = "embedding_id", nullable = false, unique = true, length = 255)
    public String embeddingId;

    @Column(name = "embedding_type", nullable = false, length = 30)
    public String embeddingType; // CONTENT or HYPOTHETICAL_QUESTION

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
