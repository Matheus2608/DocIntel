package dev.matheus.service.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class KeywordRetriever {

    private static final String FILE_NAME_KEY = "FILE_NAME";

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;


    public EmbeddingSearchResult<TextSegment> search(String filename, List<String> keywords, String query, int maxResults) {
        String safeQuery = query == null ? "" : query.trim();
        String keywordText = keywords == null ? "" : keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));

        String embeddingInput = safeQuery.isBlank() ? keywordText : safeQuery;
        Embedding embedding = embeddingModel.embed(embeddingInput).content();

        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .filter(new IsEqualTo(FILE_NAME_KEY, filename))
                .maxResults(maxResults);

        applyHybridTextQueryIfSupported(builder, keywordText);

        return embeddingStore.search(
                builder.build());
    }

    private void applyHybridTextQueryIfSupported(EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder, String keywordText) {
        if (keywordText == null || keywordText.isBlank()) {
            return;
        }

        try {
            Method queryMethod = builder.getClass().getMethod("query", String.class);
            queryMethod.invoke(builder, keywordText);
        } catch (ReflectiveOperationException ignored) {
            // Running with a LangChain4j version that does not expose text query on the builder.
        }
    }
}
