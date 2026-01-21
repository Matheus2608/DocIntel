package dev.matheus.service.retrieval;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.vertexai.VertexAiScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.matheus.dto.RetrievalSegment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Processes and scores retrieved segments.
 */
@ApplicationScoped
public class RetrievalSegmentProcessor {
    private static final Logger LOG = Logger.getLogger(RetrievalSegmentProcessor.class);
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    VertexAiScoringModel scoringModel;

    /**
     * Processes matches by scoring them and converting to RetrievalSegments.
     * Removes duplicates based on paragraph content.
     */
    public List<RetrievalSegment> processMatches(List<EmbeddingMatch<TextSegment>> matches, String question) {
        LOG.debugf("Processing %d matches for scoring", matches.size());

        List<RetrievalSegment> segments = matches.parallelStream()
                .collect(Collectors.toMap(
                        this::getParagraph,
                        match -> createRetrievalSegment(match, question),
                        (existing, replacement) -> existing // Keep first in case of duplicates
                ))
                .values()
                .stream()
                .toList();

        LOG.debugf("Processed into %d unique segments after deduplication", segments.size());
        return segments;
    }

    /**
     * Filters and sorts segments based on score threshold.
     */
    public List<RetrievalSegment> filterAndSort(List<RetrievalSegment> segments, double minScore, int maxResults) {
        LOG.debugf("Filtering and sorting %d segments with minScore=%.2f, maxResults=%d",
                (Object) segments.size(), minScore, maxResults);

        List<RetrievalSegment> filtered = segments.stream()
//                .filter(segment -> segment.modelScore() >= minScore)
                .sorted((s1, s2) -> s2.modelScore().compareTo(s1.modelScore()))
                .limit(maxResults)
                .toList();

        LOG.debugf("Returning %d filtered segments", filtered.size());
        return filtered;
    }

    private RetrievalSegment createRetrievalSegment(EmbeddingMatch<TextSegment> match, String question) {

        String paragraphFromMetadata = match.embedded().metadata().getString(PARAGRAPH_KEY);
        String paragraph = paragraphFromMetadata != null ? paragraphFromMetadata : match.embedded().text();
        String questionToBeSaved = paragraphFromMetadata != null ? match.embedded().text() : question;
        String paragraphPreview = paragraph.substring(0, Math.min(50, paragraph.length()));

        LOG.debugf("Calling VertexAI scoring for paragraph preview: %s...", paragraphPreview);

        Double score;
        try {
            var scoreResponse = scoringModel.score(paragraph, question);
            score = scoreResponse.content();
            LOG.debugf("VertexAI score received: %.4f for paragraph: %s...", score, paragraphPreview);
        } catch (Exception e) {
            LOG.errorf(e, "Error calling VertexAI scoring model for paragraph: %s...", paragraphPreview);
            throw e;
        }

        return new RetrievalSegment(
                questionToBeSaved,
                paragraph,
                match.score(),
                score
        );
    }

    private String getParagraph(EmbeddingMatch<TextSegment> match) {
        String paragraphFromMetadata = match.embedded().metadata().getString(PARAGRAPH_KEY);
        return paragraphFromMetadata != null ? paragraphFromMetadata : match.embedded().text();
    }
}

