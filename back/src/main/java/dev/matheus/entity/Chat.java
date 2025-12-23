package dev.matheus.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats")
public class Chat extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "title", nullable = false)
    public String title;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @OneToOne(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    public DocumentFile documentFile;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ChatMessage> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

