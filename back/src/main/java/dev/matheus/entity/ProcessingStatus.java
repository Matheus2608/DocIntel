package dev.matheus.entity;

/**
 * Processing status for document files in the Docling ingestion pipeline.
 * Tracks the state of document processing from upload to completion.
 */
public enum ProcessingStatus {
    /**
     * Document uploaded but not yet processed by Docling
     */
    PENDING,
    
    /**
     * Currently being processed by Docling Serve
     */
    PROCESSING,
    
    /**
     * Successfully processed and chunked
     */
    COMPLETED,
    
    /**
     * Processing failed (see processingError field for details)
     */
    FAILED
}
