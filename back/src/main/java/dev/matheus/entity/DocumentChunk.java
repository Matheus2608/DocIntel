package dev.matheus.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a semantic chunk of a processed document.
 * Chunks are created by Docling's HybridChunker based on document structure.
 */
@Entity
@Table(name = "document_chunk")
public class DocumentChunk extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_file_id", nullable = false)
    public DocumentFile documentFile;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    public ContentType contentType;

    @Column(name = "position", nullable = false)
    public Integer position;

    @Column(name = "section_heading", length = 500)
    public String sectionHeading;

    @Column(name = "heading_level")
    public Integer headingLevel;

    @Column(name = "token_count", nullable = false)
    public Integer tokenCount;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
