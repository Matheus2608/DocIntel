-- Migration: Add Docling document processing entities
-- Feature: 001-docling-ingestion
-- Date: 2025-01-29

-- Add processing status columns to document_files table
ALTER TABLE document_files
ADD COLUMN processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN processing_error TEXT,
ADD COLUMN processed_at TIMESTAMP,
ADD COLUMN chunk_count INTEGER,
ADD COLUMN processor_version VARCHAR(50);

-- Create document_chunk table
CREATE TABLE document_chunk (
    id VARCHAR(36) PRIMARY KEY,
    document_file_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL,
    section_heading VARCHAR(500),
    heading_level INTEGER,
    token_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_file_id) 
        REFERENCES document_files(id) ON DELETE CASCADE
);

-- Create indexes for document_chunk
CREATE INDEX idx_chunk_document_id ON document_chunk(document_file_id);
CREATE INDEX idx_chunk_position ON document_chunk(document_file_id, position);
CREATE INDEX idx_chunk_content_type ON document_chunk(content_type);

-- Create chunk_embedding table
CREATE TABLE chunk_embedding (
    id VARCHAR(36) PRIMARY KEY,
    chunk_id VARCHAR(36) NOT NULL,
    embedding_id VARCHAR(255) NOT NULL UNIQUE,
    embedding_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_embedding_chunk FOREIGN KEY (chunk_id) 
        REFERENCES document_chunk(id) ON DELETE CASCADE
);

-- Create indexes for chunk_embedding
CREATE INDEX idx_embedding_chunk_id ON chunk_embedding(chunk_id);
CREATE INDEX idx_embedding_id ON chunk_embedding(embedding_id);

-- Add chunk reference to hypotetical_question table (nullable for backwards compatibility)
ALTER TABLE hypotetical_question
ADD COLUMN chunk_id VARCHAR(36),
ADD CONSTRAINT fk_question_chunk FOREIGN KEY (chunk_id) 
    REFERENCES document_chunk(id) ON DELETE SET NULL;

-- Add index for chunk_id in hypotetical_question
CREATE INDEX idx_question_chunk_id ON hypotetical_question(chunk_id);
