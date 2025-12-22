-- Estrutura do Banco de Dados - DocIntel

-- Tabela de Chats
-- Armazena as sessões de chat
CREATE TABLE chats (
    id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Tabela de Mensagens do Chat
-- Armazena todas as mensagens (usuário e assistente) de cada chat
CREATE TABLE chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'user' ou 'assistant'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
);

-- Tabela de Arquivos de Documentos
-- Cada chat pode ter um documento associado (PDF ou DOCX)
CREATE TABLE document_files (
    id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
);

-- Índices para melhorar performance
CREATE INDEX idx_chat_messages_chat_id ON chat_messages(chat_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_document_files_chat_id ON document_files(chat_id);

