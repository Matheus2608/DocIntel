package dev.matheus.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_steps")
public class AgentStep extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(name = "message_id", nullable = false, length = 36)
    public String messageId;

    @Column(name = "chat_id", nullable = false, length = 36)
    public String chatId;

    @Column(name = "tool_name", nullable = false, length = 100)
    public String toolName;

    @Column(name = "status", nullable = false, length = 20)
    public String status; // running | done | error

    @Column(name = "arguments_json", columnDefinition = "TEXT")
    public String argumentsJson;

    @Column(name = "result_preview", columnDefinition = "TEXT")
    public String resultPreview;

    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    @Column(name = "sequence_idx", nullable = false)
    public int sequenceIdx;

    @Column(name = "started_at", nullable = false)
    public LocalDateTime startedAt;

    @Column(name = "ended_at")
    public LocalDateTime endedAt;

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
