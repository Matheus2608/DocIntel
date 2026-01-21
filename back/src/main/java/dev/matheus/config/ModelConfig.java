package dev.matheus.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiScoringModel;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

@ApplicationScoped
public class ModelConfig {

    @ConfigProperty(name = "quarkus.thread-pool.retrieval-executor.core-threads", defaultValue = "10")
    Integer executorThreadPoolSize;

    @ConfigProperty(name = "OPENAI_API_KEY")
    String openaiApiKey;

    @Produces
    @ApplicationScoped
    @Named("retrievalExecutorService")
    public ExecutorService retrievalExecutorService() {
        return Executors.newFixedThreadPool(executorThreadPoolSize);
    }

    @Produces
    @ApplicationScoped
    public EmbeddingModel defaultEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-small")
                .dimensions(768)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .httpClientBuilder(new JaxRsHttpClientBuilder())
                .build();
    }

    /**
     * Método comum para criar modelos de chat OpenAI com configurações base
     * @param modelName Nome do modelo OpenAI (ex: gpt-4o, gpt-4o-mini)
     * @param temperature Temperatura do modelo (0.0 a 2.0)
     * @return Instância configurada do ChatLanguageModel
     */
    private OpenAiChatModel createOpenAiChatModel(String modelName, double temperature) {
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(180))
                .maxRetries(3)
                .httpClientBuilder(new JaxRsHttpClientBuilder())
                .build();
    }

    @Produces
    @ApplicationScoped
    @ModelName("question-extractor")
    public OpenAiChatModel questionExtractorModel() {
        return createOpenAiChatModel("gpt-4o-mini", 0.7);
    }

    @Produces
    @ApplicationScoped
    @ModelName("search-strategy")
    public OpenAiChatModel searchStrategyModel() {
        return createOpenAiChatModel("gpt-4o-mini", 1.0);
    }

    @Produces
    @ApplicationScoped
    @ModelName("fake-answer-generator")
    public OpenAiChatModel fakeAnswerModel() {
        return createOpenAiChatModel("gpt-4o-mini", 0.9);
    }

    @Produces
    @ApplicationScoped
    @ModelName("scoring-model")
    public OpenAiChatModel scoringModel() {
        return createOpenAiChatModel("gpt-4o-mini", 1.0);
    }


    @Produces
    @ApplicationScoped
    public VertexAiScoringModel vertexAiScoringModel() {
        return VertexAiScoringModel.builder()
                .projectId(ensureNotNull(System.getenv("GCP_PROJECT_ID"), "GCP_PROJECT_ID"))
                .projectNumber(ensureNotNull(System.getenv("GCP_PROJECT_NUM"), "GCP_PROJECT_NUM"))
                .model("semantic-ranker-512")
                .location("southamerica-east1")
                .build();
    }
}

