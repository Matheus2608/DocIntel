package dev.matheus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.matheus.entity.AgentStep;
import dev.matheus.entity.ChatMessage;
import dev.matheus.repository.AgentStepRepository;
import dev.matheus.repository.ChatMessageRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AgentStepService {

    private static final int RESULT_PREVIEW_MAX = 2048;

    @Inject
    AgentStepRepository repository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    ChatSessionRegistry registry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String startStep(String chatId, String toolName, Map<String, Object> arguments) {
        Optional<ChatMessage> lastUser = chatMessageRepository.findLastUserMessageByChatId(chatId);
        if (lastUser.isEmpty()) {
            Log.warnf("AgentStepService.startStep could not resolve messageId for chatId=%s", chatId);
            registry.sendToolCallEvent(chatId, toolName, "start", serializeArgs(arguments));
            return null;
        }
        String messageId = lastUser.get().id;

        AgentStep step = new AgentStep();
        step.messageId = messageId;
        step.chatId = chatId;
        step.toolName = toolName;
        step.status = "running";
        String argsJson = serializeArgs(arguments);
        step.argumentsJson = argsJson;
        step.sequenceIdx = repository.nextSequenceIdx(messageId);
        step.startedAt = LocalDateTime.now();
        repository.persist(step);

        registry.sendToolCallEvent(chatId, toolName, "start", argsJson);
        return step.id;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void endStep(String stepId, String resultPreview) {
        if (stepId == null) {
            return;
        }
        AgentStep step = repository.findById(stepId);
        if (step == null) {
            return;
        }
        step.status = "done";
        step.resultPreview = truncate(resultPreview);
        step.endedAt = LocalDateTime.now();
        registry.sendToolCallEvent(step.chatId, step.toolName, "end");
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void failStep(String stepId, String errorMessage) {
        if (stepId == null) {
            return;
        }
        AgentStep step = repository.findById(stepId);
        if (step == null) {
            return;
        }
        step.status = "error";
        step.errorMessage = truncate(errorMessage);
        step.endedAt = LocalDateTime.now();
        registry.sendToolCallEvent(step.chatId, step.toolName, "end");
    }

    private String serializeArgs(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            Log.warnf("Failed to serialize agent step arguments: %s", e.getMessage());
            return null;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= RESULT_PREVIEW_MAX) {
            return value;
        }
        return value.substring(0, RESULT_PREVIEW_MAX) + "...[truncated]";
    }
}
