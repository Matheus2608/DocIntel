package dev.matheus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion when serializing Chat.messages
    public Chat chat;

    @Column(name = "role", nullable = false, length = 20)
    public String role; // "user" ou "assistant"

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "retrieval_info_id")
    public RetrievalInfo retrievalInfo;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

