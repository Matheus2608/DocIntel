package dev.matheus.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.matheus.dto.RetrievalSegment;
import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.entity.HypoteticalQuestion;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.repository.RetrievalInfoRepository;
import dev.matheus.service.retrieval.FakeAnswerRetriever;
import dev.matheus.service.retrieval.HypotheticalQuestionRetriever;
import dev.matheus.service.retrieval.RetrievalSegmentProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

enum SearchStrategy {
    @Description("Estrategia que usa o match entre questoes hipoteticas e a pergunta do usuario. Reformule bem a pergunta para melhores resultados.")
    HYPOTHETICAL_QUESTIONS,
    @Description("Estrategia que cria uma resposta falsa e a utiliza para fazer o match com os segmentos.")
    FAKE_ANSWERS,
    @Description("Usa ambas as estrategias.")
    BOTH
}

record RetrievalSearchParams(
        double minSimilarity,
        double minScore,
        int maxResults,
        SearchStrategy strategy
) {}

@ApplicationScoped
public class RetrievalInfoService {

    private static final Logger LOG = Logger.getLogger(RetrievalInfoService.class);

    @Inject
    RetrievalInfoRepository repository;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    ChatService chatService;

    @Inject
    @Named("retrievalExecutorService")
    ExecutorService executorService;

    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;

//    @Inject
//    SearchStrategyDeterminerAiService searchStrategyDeterminer;


    @Inject
    HypotheticalQuestionRetriever hypotheticalQuestionRetriever;

    @Inject
    FakeAnswerRetriever fakeAnswerRetriever;

    @Inject
    RetrievalSegmentProcessor processor;


    @Tool("Encontre conteúdos relevantes para responder perguntas relacionadas ao documento usando RAG")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String retrieveRelevantContents(
            String chatId,
            @P("duvida do usuario ou duvida reformulada para gerar melhores resultados") String question,
            @P("A similaridade mínima para a busca de segmentos relevantes (entre 0.7 e 0.9") double minSimilarity,
            @P("O score mínimo para considerar um segmento relevante (entre 0.5 e 0.8") double minScore,
            @P("O número máximo de resultados a serem retornados (entre 2 e 10") int maxResults,
            @P("A estratégia de busca a ser utilizada") SearchStrategy strategy
    ) {
        LOG.infof("Retrieving relevant contents for question: %s", question);

        ChatContext context = getChatContext(chatId);
        if (context == null) {
            return "No information found.";
        }

        RetrievalSearchParams searchParams = new RetrievalSearchParams(
                minSimilarity,
                minScore,
                maxResults,
                strategy
        );

        LOG.infof("Using strategy: %s with params: minSimilarity=%.2f, minScore=%.2f, maxResults=%d",
                strategy, minSimilarity, minScore, maxResults);

        List<EmbeddingMatch<TextSegment>> matches = performSearch(
                question, context.filename, strategy, searchParams);

        List<RetrievalSegment> segments = processor.processMatches(matches, question);

        saveRetrievalInfoAsync(context.messageId, question, segments);

        if (segments.isEmpty()) {
            LOG.warnf("No relevant contents found for question: %s", question);
            return "Não foram achados conteúdos relevantes. Talvez se reformular a pergunta consiga encontrar melhores resultados.";
        }

        List<RetrievalSegment> filtered = processor.filterAndSort(
                segments, searchParams.minScore(), searchParams.maxResults());

        return formatResults(filtered);
    }

    private ChatContext getChatContext(String chatId) {
        try {
            String messageId = getMessageId(chatId);
            String filename = chatService.getDocument(chatId).fileName();
            return new ChatContext(messageId, filename);
        } catch (NotFoundException ex) {
            LOG.warnf("No previous user message or document found for chatId=%s", chatId);
            return null;
        } catch (InterruptedException ex) {
            LOG.errorf(ex, "Interrupted while retrieving message ID for chatId=%s", chatId);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private List<EmbeddingMatch<TextSegment>> performSearch(
            String question,
            String filename,
            SearchStrategy strategy,
            RetrievalSearchParams params
    ) {
        LOG.debugf("Performing search with strategy=%s for filename=%s", strategy, filename);

        LOG.debug("Generating embedding for question...");
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        LOG.debug("Question embedding generated successfully");

        List<CompletableFuture<EmbeddingSearchResult<TextSegment>>> searchFutures = new ArrayList<>();

        if (strategy == SearchStrategy.HYPOTHETICAL_QUESTIONS || strategy == SearchStrategy.BOTH) {
            LOG.debug("Adding HYPOTHETICAL_QUESTIONS strategy to search");
            searchFutures.add(CompletableFuture.supplyAsync(
                    () -> {
                        LOG.debug("Executing HYPOTHETICAL_QUESTIONS search...");
                        EmbeddingSearchResult<TextSegment> result = hypotheticalQuestionRetriever.search(
                                questionEmbedding, filename, params.maxResults(), params.minSimilarity());
                        LOG.debugf("HYPOTHETICAL_QUESTIONS search completed with %d results", result.matches().size());
                        return result;
                    },
                    executorService));
        }

        if (strategy == SearchStrategy.FAKE_ANSWERS || strategy == SearchStrategy.BOTH) {
            LOG.debug("Adding FAKE_ANSWERS strategy to search");
            searchFutures.add(CompletableFuture.supplyAsync(
                    () -> {
                        LOG.debug("Executing FAKE_ANSWERS search...");
                        EmbeddingSearchResult<TextSegment> result = fakeAnswerRetriever.search(
                                question, filename, params.maxResults(), params.minSimilarity());
                        LOG.debugf("FAKE_ANSWERS search completed with %d results", result.matches().size());
                        return result;
                    },
                    executorService));
        }

        LOG.debug("Waiting for all search futures to complete...");
        CompletableFuture.allOf(searchFutures.toArray(new CompletableFuture[0])).join();
        LOG.debug("All search futures completed");

        LOG.debug("Collecting results from futures...");
        List<EmbeddingMatch<TextSegment>> matches = searchFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(result -> result.matches().stream())
                .toList();

        LOG.debugf("Search strategies completed, found %d total matches", matches.size());
        return matches;
    }

    private String formatResults(List<RetrievalSegment> segments) {
        return segments.stream()
                .map(seg -> String.format("SCORE: %.2f\nChunk: %s", seg.modelScore(), seg.chunk()))
                .collect(Collectors.joining(
                        "\n---\n",
                        "Segue segmentos ordenados em ordem de importância.\n---\n",
                        "\n---\n"));
    }

    private void saveRetrievalInfoAsync(String messageId, String question, List<RetrievalSegment> segments) {
        CompletableFuture.runAsync(() -> {
            try {
                saveRetrievalInfo(new RetrievalInfoSaveRequest(messageId, question, segments));
            } catch (Exception e) {
                LOG.errorf(e, "Error saving retrieval info asynchronously");
            }
        }, executorService);
    }

    private String getMessageId(String chatId) throws InterruptedException {
        Thread.sleep(400);
        return retryGetMessageId(chatId);
    }

    @Retry(maxRetries = 2, delay = 100)
    String retryGetMessageId(String chatId) {
        return chatService.getLastUserMessage(chatId).id();
    }

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

    private record ChatContext(String messageId, String filename) {}
}
