package dev.matheus.service;

import dev.matheus.dto.Question;
import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.entity.HypoteticalQuestion;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.rag.RagIngestion;
import dev.matheus.repository.RetrievalInfoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class RetrievalInfoService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);

    @Inject
    private RetrievalInfoRepository repository;

    @Inject
    public RagIngestion ragIngestion;

    @Transactional
    public void saveRetrievalInfo(RetrievalInfoSaveRequest request) {
        LOG.infof("Saving RetrievalInfo for ChatMessage ID: %s", request.chatMessageId());
        try {
            var chatMessage = repository.findByChatMessageId(request.chatMessageId());
            if (chatMessage == null) {
                throw new NotFoundException("ChatMessage not found with id: " + request.chatMessageId());
            }

            // Check if RetrievalInfo already exists
            if (chatMessage.retrievalInfo != null) {
                LOG.warnf("RetrievalInfo already exists for ChatMessage ID: %s", request.chatMessageId());
                return;
            }

            RetrievalInfo retrievalInfo = new RetrievalInfo();
            retrievalInfo.userQuestion = request.userQuestion();

            // Set bidirectional relationship correctly
            List<HypoteticalQuestion> questions = request.hypoteticalQuestions().stream().map(question -> {
                HypoteticalQuestion hq = new HypoteticalQuestion();
                hq.question = question.question();
                hq.similarityScore = question.similarity().toString();
                hq.chunk = question.chunk();
                hq.retrievalInfo = retrievalInfo; // Set the parent reference
                return hq;
            }).toList();

            retrievalInfo.hypoteticalQuestions = questions;
            chatMessage.retrievalInfo = retrievalInfo;

            // Just set the relationship - cascade will handle persistence
            LOG.infof("RetrievalInfo saved successfully for ChatMessage ID: %s with %d questions",
                     request.chatMessageId(), questions.size());
        } catch (Exception e) {
            LOG.errorf(e, "Error saving RetrievalInfo for ChatMessage ID: %s", request.chatMessageId());
            throw e;
        }
    }

    public RetrievalInfo getRetrievalInfoByChatMessageId(String chatMessageId) {
        LOG.infof("Fetching RetrievalInfo for ChatMessage ID: %s", chatMessageId);
        var chatMessage = repository.findByChatMessageId(chatMessageId);
        if (chatMessage == null || chatMessage.retrievalInfo == null) {
            throw new NotFoundException("RetrievalInfo not found for ChatMessage ID: " + chatMessageId);
        }
        return chatMessage.retrievalInfo;
    }

    @Transactional
    public void retrieveAndSaveInfo(String chatMessageId) {
        LOG.infof("Retrieving and saving info for ChatMessage ID: %s", chatMessageId);

        var chatMessage = repository.findByChatMessageId(chatMessageId);
        if (chatMessage == null) {
            throw new NotFoundException("ChatMessage not found with id: " + chatMessageId);
        }

        if (chatMessage.content == null || chatMessage.content.trim().isEmpty()) {
            throw new IllegalArgumentException("ChatMessage content is empty for id: " + chatMessageId);
        }

        String userQuestion = chatMessage.content;
        LOG.infof("User question from message: %s", userQuestion);

        List<Question> questions = ragIngestion.retrieveQuestions(userQuestion);
        LOG.infof("Retrieved %d questions for messageId=%s",
                 questions != null ? questions.size() : 0, chatMessageId);

        if (questions == null || questions.isEmpty()) {
            LOG.warnf("No questions retrieved for ChatMessage ID: %s", chatMessageId);
            throw new NotFoundException("No relevant content found for this question");
        }

        saveRetrievalInfo(
                new RetrievalInfoSaveRequest(
                        chatMessageId,
                        userQuestion,
                        questions
                )
        );
    }
}
