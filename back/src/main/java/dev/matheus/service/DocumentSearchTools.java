package dev.matheus.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.matheus.ai.TranslationAiService;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.dto.RetrievalSegment;
import dev.matheus.service.retrieval.FakeAnswerRetriever;
import dev.matheus.service.retrieval.HypotheticalQuestionRetriever;
import dev.matheus.service.retrieval.KeywordRetriever;
import dev.matheus.service.retrieval.RetrievalSegmentProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@ApplicationScoped
public class DocumentSearchTools {

    private static final Logger LOG = Logger.getLogger(DocumentSearchTools.class);
    private static final double MIN_SCORE = 0.6;

    @Inject
    HypotheticalQuestionRetriever hypotheticalRetriever;

    @Inject
    FakeAnswerRetriever fakeAnswerRetriever;

    @Inject
    KeywordRetriever keywordRetriever;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    RetrievalInfoService retrievalInfoService;

    @Inject
    ChatService chatService;

    @Inject
    RetrievalSegmentProcessor processor;

    @Inject
    @Named("retrievalExecutorService")
    ExecutorService executorService;

    @Inject
    TranslationAiService translationService;

    @Inject
    AgentStepService agentStepService;

    @Tool("Use para perguntas factuais diretas")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String searchByHypotheticalQuestions(String chatId, @P("pergunta") String question, double minSimilarity, int maxResults) {
        String stepId = agentStepService.startStep(chatId, "searchByHypotheticalQuestions",
                java.util.Map.of("question", question, "minSimilarity", minSimilarity, "maxResults", maxResults));
        try {
            ChatContext ctx = getChatContext(chatId);
            if (ctx == null) {
                agentStepService.endStep(stepId, "No information found.");
                return "No information found.";
            }

            String translatedQuestion = translateIfNeeded(question, ctx.language());
            Embedding emb = embeddingModel.embed(translatedQuestion).content();
            EmbeddingSearchResult<TextSegment> result = hypotheticalRetriever.search(emb, ctx.filename(), maxResults, minSimilarity);
            String formatted = processAndFormat(ctx, translatedQuestion, result.matches(), maxResults);
            agentStepService.endStep(stepId, formatted);
            return formatted;
        } catch (RuntimeException ex) {
            agentStepService.failStep(stepId, ex.getMessage());
            throw ex;
        }
    }

    @Tool("Use para perguntas abertas ou conceituais.")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String searchByFakeAnswer(String chatId, @P("pergunta") String question, double minSimilarity, int maxResults) {
        String stepId = agentStepService.startStep(chatId, "searchByFakeAnswer",
                java.util.Map.of("question", question, "minSimilarity", minSimilarity, "maxResults", maxResults));
        try {
            ChatContext ctx = getChatContext(chatId);
            if (ctx == null) {
                agentStepService.endStep(stepId, "No information found.");
                return "No information found.";
            }

            String translatedQuestion = translateIfNeeded(question, ctx.language());
            EmbeddingSearchResult<TextSegment> result = fakeAnswerRetriever.search(translatedQuestion, ctx.filename(), maxResults, minSimilarity);
            String formatted = processAndFormat(ctx, translatedQuestion, result.matches(), maxResults);
            agentStepService.endStep(stepId, formatted);
            return formatted;
        } catch (RuntimeException ex) {
            agentStepService.failStep(stepId, ex.getMessage());
            throw ex;
        }
    }

    @Tool("Use quando a pergunta contém termos técnicos, nomes próprios ou identificadores")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String searchByKeyword(String chatId, @P("palavras-chave") List<String> keywords, @P("consulta") String query, int maxResults) {
        String stepId = agentStepService.startStep(chatId, "searchByKeyword",
                java.util.Map.of("keywords", keywords == null ? List.of() : keywords, "query", query, "maxResults", maxResults));
        try {
            ChatContext ctx = getChatContext(chatId);
            if (ctx == null) {
                agentStepService.endStep(stepId, "No information found.");
                return "No information found.";
            }

            List<String> safeKeywords = keywords == null ? List.of() : keywords;
            List<String> translatedKeywords = safeKeywords.stream().map(k -> translateIfNeeded(k, ctx.language())).toList();
            String translatedQuery = translateIfNeeded(query, ctx.language());
            EmbeddingSearchResult<TextSegment> result = keywordRetriever.search(ctx.filename(), translatedKeywords, translatedQuery, maxResults);
            String formatted = processAndFormat(ctx, translatedQuery, result.matches(), maxResults);
            agentStepService.endStep(stepId, formatted);
            return formatted;
        } catch (RuntimeException ex) {
            agentStepService.failStep(stepId, ex.getMessage());
            throw ex;
        }
    }

    private String translateIfNeeded(String text, String docLanguage) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (docLanguage == null || docLanguage.isBlank() || docLanguage.equalsIgnoreCase("pt")) {
            return text;
        }
        try {
            String translated = translationService.translate(text, docLanguage);
            return (translated == null || translated.isBlank()) ? text : translated;
        } catch (Exception ex) {
            LOG.warnf(ex, "Translation failed for language=%s, fallback to original text", docLanguage);
            return text;
        }
    }

    private String processAndFormat(ChatContext ctx, String question, List<EmbeddingMatch<TextSegment>> matches, int maxResults) {
        List<RetrievalSegment> segments = processor.processMatches(matches, question, maxResults);

        if (segments.isEmpty()) {
            saveRetrievalInfoAsync(ctx.messageId(), question, List.of());
            return "Não foram achados conteúdos relevantes. Reformule a pergunta ou reduza a similaridade mínima.";
        }

        List<RetrievalSegment> finalSegments = segments.size() > maxResults
                ? processor.filterAndSort(segments, MIN_SCORE, maxResults)
                : segments;
        saveRetrievalInfoAsync(ctx.messageId(), question, finalSegments);
        return formatResults(finalSegments);
    }

    private String formatResults(List<RetrievalSegment> segments) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            out.append("[").append(i + 1).append("] ").append(segments.get(i).chunk()).append("\n---\n");
        }
        return out.toString();
    }

    private ChatContext getChatContext(String chatId) {
        try {
            String messageId = getMessageId(chatId);
            DocumentFileResponse doc = chatService.getDocument(chatId);
            return new ChatContext(messageId, doc.fileName(), doc.language());
        } catch (NotFoundException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warnf("No context found for chatId=%s", chatId);
            return null;
        }
    }

    private String getMessageId(String chatId) throws InterruptedException {
        Thread.sleep(400);
        return retryGetMessageId(chatId);
    }

    @Retry(maxRetries = 2, delay = 100)
    String retryGetMessageId(String chatId) {
        return chatService.getLastUserMessage(chatId).id();
    }

    private void saveRetrievalInfoAsync(String messageId, String question, List<RetrievalSegment> segments) {
        CompletableFuture.runAsync(() -> {
            try {
                retrievalInfoService.saveRetrievalInfo(new RetrievalInfoSaveRequest(messageId, question, segments));
            } catch (Exception e) {
                LOG.errorf(e, "Error saving retrieval info");
            }
        }, executorService);
    }

    private record ChatContext(String messageId, String filename, String language) {
    }
}
