package dev.matheus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_files")
public class DocumentFile extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false, unique = true)
    @JsonIgnore // Prevent infinite recursion when serializing Chat.documentFile
    public Chat chat;

    @Column(name = "file_name", nullable = false)
    public String fileName;

    @Column(name = "file_type", nullable = false, length = 50)
    public String fileType; // "application/pdf" ou "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    @Column(name = "file_size", nullable = false)
    public Long fileSize;

    @Lob
    @Column(name = "file_data", nullable = false)
    public byte[] fileData;

    @Column(name = "uploaded_at", nullable = false)
    public LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
    }
}

