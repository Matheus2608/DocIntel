-- Add columns for reranking scores and positions to hypotetical_question table
-- Run this migration to support the new reranking feature

ALTER TABLE hypotetical_question
    ADD COLUMN IF NOT EXISTS model_score DOUBLE PRECISION;

-- Add comment to document the columns
-- COMMENT ON COLUMN hypotetical_question.model_score IS 'Reranking model score (0.0-1.0) indicating relevance to user question';