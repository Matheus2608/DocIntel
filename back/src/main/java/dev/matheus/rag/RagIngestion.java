package dev.matheus.rag;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.dto.Question;
import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.service.ChatService;
import dev.matheus.service.RetrievalInfoService;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilder;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record QuestionParagraph(
        String question,
        TextSegment paragraph
) {
}

@ApplicationScoped
public class RagIngestion {

    private static final Gson gson = new Gson();
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String MODEL_EMBEDDING_TEXT = "text-embedding-3-small";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    final EmbeddingStore<TextSegment> embeddingStore;
    final EmbeddingModel embeddingModel;
    final ChatService chatService;
    final RetrievalInfoService retrievalInfoService;
    final Map<String, ContentRetriever> retrievers;
    final OpenAiChatModel openAiChatModel;

    public RagIngestion(EmbeddingStore<TextSegment> embeddingStore, ChatService chatService, RetrievalInfoService retrievalInfoService) {
        this.retrievalInfoService = retrievalInfoService;
        this.embeddingStore = embeddingStore;
        this.chatService = chatService;
        this.retrievers = new HashMap<>();
        this.openAiChatModel = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .modelName("gpt-4o")
                .httpClientBuilder(new JaxRsHttpClientBuilder())
                .maxRetries(1)
                .temperature(0.4)
//                .responseFormat()
//                .strictJsonSchema()
                .build();

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .dimensions(768)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Tool("Retrieve relevant contents to answer document related questions")
    @Transactional
    public String retrieveRelevantContents(String userQuestion, String chatId) {
        Log.infof("User question: %s", userQuestion);

        EmbeddingSearchResult<TextSegment> searchResults = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .maxResults(4)
                        .minScore(0.7)
                        .queryEmbedding(embeddingModel.embed(userQuestion).content())
                        .build());

        List<Question> questions = searchResults.matches().stream()
                .map(match -> new Question(
                        userQuestion,
                        match.embedded().metadata().getString(PARAGRAPH_KEY),
                        match.score()
                )).toList();

        String messageId;
        try {
            messageId = chatService.getLastUserMessage(chatId).id();
        } catch (NotFoundException ex) {
            Log.warnf("No previous user message found for chatId=%s. Skipping retrieval info save.", chatId);
            return "No information found.";
        }

        retrievalInfoService.saveRetrievalInfo(new RetrievalInfoSaveRequest(
                messageId,
                userQuestion,
                questions
        ));

        if (questions.isEmpty()) {
            Log.warnf("No relevant contents found for user question='%s'", userQuestion);
            return "No relevant contents found.";
        }

        String concatenatedExtracts = searchResults.matches().stream()
                .map(match -> match.embedded().metadata().getString(PARAGRAPH_KEY))
                .distinct()
                .collect(Collectors.joining("\n---\n", "\n---\n", "\n---\n"));

        System.out.println("\nResponse:\n" + concatenatedExtracts + "\n");
        return concatenatedExtracts;
    }

    public List<Question> retrieveQuestions(String userQuestion) {
        Log.infof("User question: %s", userQuestion);

        EmbeddingSearchResult<TextSegment> searchResults = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .maxResults(4)
                        .minScore(0.7)
                        .queryEmbedding(embeddingModel.embed(userQuestion).content())
                        .build());

        return searchResults.matches().stream()
                .map(match -> new Question(
                        match.embedded().text(),
                        match.embedded().metadata().getString(PARAGRAPH_KEY),
                        match.score()
                )).toList();
    }

    public void ingestionOfHypotheticalQuestions(byte[] docBytes) {
        System.out.println("\u001B[36m" + "Ingesting hypothetical questions..." + "\u001B[0m");

        Document document = toDocument(docBytes);
        List<QuestionParagraph> allQuestionParagraphs = new ArrayList<>();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(2000, 100);
        List<TextSegment> paragraphs = splitter.split(document);

        for (TextSegment paragraphSegment : paragraphs) {
            System.out.println("\u001B[36m" + "\n==== PARAGRAPH ==================================\n" + "\u001B[0m" + paragraphSegment.text());

            ChatResponse aiResult = openAiChatModel.chat(List.of(
                    SystemMessage.from("""
                            Suggest 2 clear questions whose answer could be given by the user provided text.
                            Don't use pronouns, be explicit about the subjects and objects of the question.
                            Return a JSON array of strings with the questions.
                            """),
                    UserMessage.from(paragraphSegment.text())
            ));

            String[] questions = toCleanedQuestions(aiResult.aiMessage().text());

            System.out.println("\u001B[33m" + "\nQUESTIONS:\n" + "\u001B[0m");
            for (int i = 0; i < questions.length; i++) {
                String question = questions[i];
                System.out.println((i + 1) + ") " + question);
                allQuestionParagraphs.add(new QuestionParagraph(question, paragraphSegment));
            }
        }

        List<TextSegment> embeddedSegments = allQuestionParagraphs.stream()
                .map(questionParagraph -> TextSegment.from(
                        questionParagraph.question(),
                        new Metadata().put(PARAGRAPH_KEY, questionParagraph.paragraph().text())))
                .toList();

        System.out.println("\u001B[36m" + "Embedding " + embeddedSegments.size() + " question-paragraph pairs..." + "\u001B[0m");
        List<Embedding> embeddings = embeddingModel.embedAll(embeddedSegments).content();
        embeddingStore.addAll(embeddings, embeddedSegments);

        System.out.println("\u001B[32m" + "Successfully ingested " + embeddedSegments.size() + " embeddings!" + "\u001B[0m");
    }

    private Document toDocument(byte[] docBytes) {
        String documentText = new String(docBytes, StandardCharsets.UTF_8);
        return Document.from(documentText);
    }

    private String[] toCleanedQuestions(String responseText) {
        // Remover markdown se houver
        String cleanedText = responseText.strip();
        if (cleanedText.startsWith("```json")) {
            cleanedText = cleanedText.substring(7).strip();
        }
        if (cleanedText.endsWith("```")) {
            cleanedText = cleanedText.substring(0, cleanedText.length() - 3).strip();
        }
        return gson.fromJson(cleanedText, String[].class);
    }
}

