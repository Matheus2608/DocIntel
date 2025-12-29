package dev.matheus.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retrieval_info")
public class RetrievalInfo extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(name = "user_question", nullable = false, columnDefinition = "TEXT")
    public String userQuestion;

    // Relacionamento bidirecional NECESSÁRIO - precisamos acessar as perguntas a partir do RetrievalInfo
    @OneToMany(mappedBy = "retrievalInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HypoteticalQuestion> hypoteticalQuestions = new ArrayList<>();
}
