-- Estrutura do Banco de Dados - DocIntel

-- Tabela de Chats
-- Armazena as sessões de chat
CREATE TABLE IF NOT EXISTS chats (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT 'Novo Chat',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Tabela de Arquivos de Documentos
-- Cada chat pode ter um documento associado (PDF ou DOCX)
CREATE TABLE IF NOT EXISTS document_files (
    id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
);

-- Tabela de Informações de Recuperação (RAG)
-- Armazena informações sobre o contexto recuperado para responder perguntas
CREATE TABLE IF NOT EXISTS retrieval_info (
    id VARCHAR(36) PRIMARY KEY,
    user_question TEXT NOT NULL,
);

-- Tabela de Perguntas Hipotéticas
-- Armazena perguntas hipotéticas geradas para cada chunk de documento (HyDE)
CREATE TABLE IF NOT EXISTS hypotetical_question (
    id BIGSERIAL PRIMARY KEY,
    retrieval_info_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    chunk TEXT NOT NULL,
    similarity_score VARCHAR(50) NOT NULL,
    FOREIGN KEY (retrieval_info_id) REFERENCES retrieval_info(id) ON DELETE CASCADE
);

-- Tabela de Mensagens do Chat
-- Armazena todas as mensagens (usuário e assistente) de cada chat
CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'user' ou 'assistant'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    retrieval_info_id VARCHAR(36), -- Apenas mensagens do tipo 'user' terão retrieval_info
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    FOREIGN KEY (retrieval_info_id) REFERENCES retrieval_info(id) ON DELETE SET NULL
);

-- Índices para melhorar performance
CREATE INDEX IF NOT EXISTS idx_chat_messages_chat_id ON chat_messages(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_retrieval_info_id ON chat_messages(retrieval_info_id);
CREATE INDEX IF NOT EXISTS idx_document_files_chat_id ON document_files(chat_id);
CREATE INDEX IF NOT EXISTS idx_hypotetical_question_retrieval_info_id ON hypotetical_question(retrieval_info_id);

