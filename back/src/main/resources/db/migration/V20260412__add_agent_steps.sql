-- Migration: Persist agent tool-call steps per user message
-- Date: 2026-04-12

CREATE TABLE agent_steps (
    id              VARCHAR(36)  PRIMARY KEY,
    message_id      VARCHAR(36)  NOT NULL,
    chat_id         VARCHAR(36)  NOT NULL,
    tool_name       VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    arguments_json  TEXT,
    result_preview  TEXT,
    error_message   TEXT,
    sequence_idx    INTEGER      NOT NULL,
    started_at      TIMESTAMP    NOT NULL,
    ended_at        TIMESTAMP,
    CONSTRAINT fk_agent_step_message FOREIGN KEY (message_id)
        REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_step_chat FOREIGN KEY (chat_id)
        REFERENCES chats(id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_steps_message_id ON agent_steps(message_id);
CREATE INDEX idx_agent_steps_chat_id    ON agent_steps(chat_id);
