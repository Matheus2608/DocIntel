package dev.matheus.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Retry;

@RegisterAiService(modelName = "scoring-model")
@ApplicationScoped
public interface ScoringModel {

    @SystemMessage("""
            You are a scoring model that rates the relevance of a given chunk information to a user question on a scale from 1 to 10.
            You must use decimal values.
            A score of 1 indicates that the response is completely irrelevant, while a score of 10 indicates that the response is perfectly relevant.
            Provide only the numerical score without any additional text or explanation.
            """)
    @UserMessage("""
            Given the question:
            {{question}}
            And the chunk retrieved to give information to the question:
            {{chunk}}
            Provide a relevance score from 1 to 10.
            """)
    @Retry
    double score(@V("question") String question, @V("chunk") String chunk);
}