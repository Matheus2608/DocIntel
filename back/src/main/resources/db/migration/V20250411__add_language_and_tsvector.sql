ALTER TABLE document_files
    ADD COLUMN IF NOT EXISTS language VARCHAR(10);

