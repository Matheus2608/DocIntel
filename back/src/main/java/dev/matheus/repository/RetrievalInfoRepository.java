package dev.matheus.repository;

import dev.matheus.entity.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RetrievalInfoRepository implements PanacheRepositoryBase<ChatMessage, String> {

    public ChatMessage findByChatMessageId(String chatMessageId) {
        return find("id", chatMessageId).firstResult();
    }
}

