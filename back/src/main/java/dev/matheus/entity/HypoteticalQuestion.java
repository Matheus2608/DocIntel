package dev.matheus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "hypotetical_question")
public class HypoteticalQuestion extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    public String question;

    @Column(name = "chunk", nullable = false, columnDefinition = "TEXT")
    public String chunk;

    @Column(name = "similarity_score", nullable = false)
    public String similarityScore;

    // Relacionamento bidirecional - cada pergunta pertence a um RetrievalInfo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrieval_info_id", nullable = false)
    @JsonIgnore // Prevent infinite recursion during JSON serialization
    public RetrievalInfo retrievalInfo;
}
