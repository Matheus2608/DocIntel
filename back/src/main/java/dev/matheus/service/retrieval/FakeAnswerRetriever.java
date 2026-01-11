package dev.matheus.service.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.matheus.ai.FakeAnswerAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Handles fake answer retrieval strategy (HyDE - Hypothetical Document Embeddings).
 */
@ApplicationScoped
public class FakeAnswerRetriever {
    private static final Logger LOG = Logger.getLogger(FakeAnswerRetriever.class);
    private static final String FILE_NAME_KEY = "FILE_NAME";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    FakeAnswerAiService fakeAnswerAiService;

    /**
     * Generates a fake answer for the question and searches using it.
     * Only returns original paragraphs (without PARAGRAPH_KEY in metadata).
     */
    public EmbeddingSearchResult<TextSegment> search(
            String question,
            String filename,
            int maxResults,
            double minSimilarity
    ) {
        LOG.debugf("FakeAnswer search - filename=%s, maxResults=%d, minSimilarity=%.2f",
                filename, maxResults, minSimilarity);
        LOG.debugf("Generating fake answer for question: %s", question);

        String fakeAnswer = fakeAnswerAiService.fakeAnswer(question);
        LOG.debugf("Fake answer generated (length=%d): %s",
                fakeAnswer.length(),
                fakeAnswer.substring(0, Math.min(100, fakeAnswer.length())));

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .maxResults(maxResults * 2)
                        .minScore(minSimilarity)
                        .queryEmbedding(embeddingModel.embed(fakeAnswer).content())
                        .filter(new IsEqualTo(FILE_NAME_KEY, filename))
                        .build()
        );

        LOG.debugf("Raw search returned %d results", result.matches().size());

        List<EmbeddingMatch<TextSegment>> filteredMatches = result.matches().stream()
                .filter(match -> !match.embedded().metadata().containsKey(PARAGRAPH_KEY))
                .limit(maxResults)
                .toList();

        LOG.debugf("After filtering without PARAGRAPH_KEY: %d results", filteredMatches.size());

        return new EmbeddingSearchResult<>(filteredMatches);
    }
}
