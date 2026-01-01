package dev.matheus.repository;

import dev.matheus.entity.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class RetrievalInfoRepository implements PanacheRepositoryBase<ChatMessage, String> {

    public ChatMessage findByChatMessageId(String chatMessageId) {
        return find("id", chatMessageId).firstResult();
    }

    public Optional<ChatMessage> findUserChatMessageByChatMessageId(String chatMessageId) {
        return find("id = ?1 and role = ?2", chatMessageId, "user").firstResultOptional();
    }
}

