package dev.matheus.entity;

/**
 * Content type classification for document chunks.
 * Used to identify the semantic type of chunk content for specialized processing.
 */
public enum ContentType {
    /**
     * Regular paragraph text content
     */
    TEXT,
    
    /**
     * Markdown-formatted table
     */
    TABLE,
    
    /**
     * Bullet or numbered list
     */
    LIST,
    
    /**
     * Code block or snippet
     */
    CODE,
    
    /**
     * Section heading (H1-H6)
     */
    HEADING,
    
    /**
     * Chunk containing multiple content types
     */
    MIXED
}
