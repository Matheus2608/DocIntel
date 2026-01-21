package dev.matheus.service.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Handles embedding-based search operations using hypothetical questions strategy.
 */
@ApplicationScoped
public class HypotheticalQuestionRetriever {
    private static final Logger LOG = Logger.getLogger(HypotheticalQuestionRetriever.class);
    private static final String FILE_NAME_KEY = "FILE_NAME";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;


    /**
     * Searches for segments using the hypothetical questions strategy.
     * Only returns segments that have a PARAGRAPH_KEY in metadata.
     */
    public EmbeddingSearchResult<TextSegment> search(
            Embedding questionEmbedding,
            String filename,
            int maxResults,
            double minSimilarity
    ) {
        LOG.debugf("HypotheticalQuestion search - filename=%s, maxResults=%d, minSimilarity=%.2f",
                filename, maxResults, minSimilarity);

        LOG.debug("Executing embedding store search...");
        EmbeddingSearchResult<TextSegment> result;
        try {
            result = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .maxResults(maxResults * 2)
                            .minScore(minSimilarity)
                            .queryEmbedding(questionEmbedding)
                            .filter(new IsEqualTo(FILE_NAME_KEY, filename))
                            .build()
            );
            LOG.debugf("Raw search returned %d results", result.matches().size());
        } catch (Exception e) {
            LOG.errorf(e, "Error during embedding search for hypothetical questions");
            throw e;
        }

        LOG.debug("Filtering results with PARAGRAPH_KEY...");
        List<EmbeddingMatch<TextSegment>> filteredMatches = result.matches().stream()
                .filter(match -> match.embedded().metadata().containsKey(PARAGRAPH_KEY))
                .limit(maxResults)
                .toList();

        LOG.debugf("After filtering with PARAGRAPH_KEY: %d results", filteredMatches.size());

        return new EmbeddingSearchResult<>(filteredMatches);
    }
}
