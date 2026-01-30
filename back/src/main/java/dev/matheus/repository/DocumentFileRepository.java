package dev.matheus.repository;

import dev.matheus.dto.DocumentStatusDTO;
import dev.matheus.entity.DocumentFile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@ApplicationScoped
public class DocumentFileRepository implements PanacheRepositoryBase<DocumentFile, String> {

    @Inject
    EntityManager em;

    public Optional<DocumentFile> findByChatId(String chatId) {
        return find("chat.id", chatId).firstResultOptional();
    }
    
    /**
     * Find document processing status by chat ID without loading the large fileData BLOB.
     * This prevents LOB stream errors when only status information is needed.
     */
    public Optional<DocumentStatusDTO> findStatusByChatId(String chatId) {
        var query = em.createQuery(
            "SELECT new dev.matheus.dto.DocumentStatusDTO(df.processingStatus, df.processingError, df.chunkCount, df.processedAt) " +
            "FROM DocumentFile df WHERE df.chat.id = :chatId", 
            DocumentStatusDTO.class);
        query.setParameter("chatId", chatId);
        
        var results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}

