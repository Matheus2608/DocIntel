package dev.matheus.repository;

import dev.matheus.dto.ChatListResponse;
import dev.matheus.entity.Chat;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChatRepository implements PanacheRepositoryBase<Chat, String> {

    public List<ChatListResponse> findAllIdAndTitle() {
        return find("SELECT c.id, c.title FROM Chat c ORDER BY c.createdAt DESC")
                .project(ChatListResponse.class)
                .list();
    }
}