package dev.matheus.service;

import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.entity.HypoteticalQuestion;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.repository.RetrievalInfoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class RetrievalInfoService {

    private static final Logger LOG = Logger.getLogger(RetrievalInfoService.class);

    @Inject
    RetrievalInfoRepository repository;

    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;

    @Transactional
    public void saveRetrievalInfo(RetrievalInfoSaveRequest request) {
        LOG.infof("Saving RetrievalInfo for ChatMessage ID: %s", request.chatMessageId());
        try {
            var chatMessage = repository.findByChatMessageId(request.chatMessageId());
            if (chatMessage == null) {
                throw new NotFoundException("ChatMessage not found with id: " + request.chatMessageId());
            }

            if (chatMessage.retrievalInfo != null) {
                LOG.warnf("RetrievalInfo already exists for ChatMessage ID: %s", request.chatMessageId());
                return;
            }

            RetrievalInfo retrievalInfo = new RetrievalInfo();
            retrievalInfo.userQuestion = request.userQuestion();

            List<HypoteticalQuestion> questions = request.retrievalSegments().stream().map(segment -> {
                HypoteticalQuestion hq = new HypoteticalQuestion();
                hq.question = segment.question();
                hq.similarityScore = segment.similarity().toString();
                hq.chunk = segment.chunk();
                hq.modelScore = segment.modelScore();
                hq.retrievalInfo = retrievalInfo;
                return hq;
            }).toList();

            retrievalInfo.hypoteticalQuestions = questions;
            chatMessage.retrievalInfo = retrievalInfo;

            repository.persist(chatMessage);

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

    public void ingestionOfHypotheticalQuestions(byte[] docBytes, String fileName, String fileType) throws IOException {
        hypotheticalQuestionService.ingestHypotheticalQuestions(docBytes, fileName, fileType);
    }
}
