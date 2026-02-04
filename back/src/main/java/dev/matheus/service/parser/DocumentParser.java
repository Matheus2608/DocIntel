package dev.matheus.service.parser;

import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;

import java.util.List;

/**
 * Interface for document parsing implementations.
 * Allows switching between different parsing engines (Docling, Textract, etc.)
 * without changing business logic.
 */
public interface DocumentParser {
    
    /**
     * Parse a document and extract chunks with structured content.
     *
     * @param documentFile The document file entity
     * @param documentContent The document content as byte array
     * @return List of document chunks with extracted content
     * @throws RuntimeException if parsing fails
     */
    List<DocumentChunk> parse(DocumentFile documentFile, byte[] documentContent);
}
