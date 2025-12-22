package dev.matheus.repository;

import dev.matheus.entity.ChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

import static java.util.Collections.list;

@ApplicationScoped
public class ChatMessageRepository implements PanacheRepositoryBase<ChatMessage, String> {

    public List<ChatMessage> findByChatId(String chatId) {
        return list("chat.id = ?1 order by createdAt", chatId);
    }
}

