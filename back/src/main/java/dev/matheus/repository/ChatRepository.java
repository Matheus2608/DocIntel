package dev.matheus.repository;

import dev.matheus.entity.Chat;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChatRepository implements PanacheRepositoryBase<Chat, String> {
}

