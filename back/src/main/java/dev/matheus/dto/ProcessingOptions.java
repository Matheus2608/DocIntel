package dev.matheus.dto;

/**
 * Request DTO for document processing options.
 */
public class ProcessingOptions {
    public String chunkingStrategy = "HYBRID"; // HIERARCHICAL | HYBRID
    public Integer maxTokens = 2000; // 100-8000
    public Boolean generateHypotheticalQuestions = true;
}
