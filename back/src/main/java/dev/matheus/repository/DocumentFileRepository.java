package dev.matheus.repository;

import dev.matheus.entity.DocumentFile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class DocumentFileRepository implements PanacheRepositoryBase<DocumentFile, String> {

    public Optional<DocumentFile> findByChatId(String chatId) {
        return find("chat.id", chatId).firstResultOptional();
    }
}

