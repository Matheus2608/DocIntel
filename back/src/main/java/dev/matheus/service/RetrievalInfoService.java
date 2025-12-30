package dev.matheus.service;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.dto.Question;
import dev.matheus.dto.RetrievalInfoSaveRequest;
import dev.matheus.entity.HypoteticalQuestion;
import dev.matheus.entity.RetrievalInfo;
import dev.matheus.repository.RetrievalInfoRepository;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record QuestionParagraph(
        String question,
        TextSegment paragraph
) { }


@ApplicationScoped
public class RetrievalInfoService {

    private static final Logger Log = Logger.getLogger(ChatService.class);

    private static final Gson gson = new Gson();
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String MODEL_EMBEDDING_TEXT = "text-embedding-3-small";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";


    final RetrievalInfoRepository repository;
    final EmbeddingStore<TextSegment> embeddingStore;
    final EmbeddingModel embeddingModel;
    final ChatService chatService;
    final Map<String, ContentRetriever> retrievers;
    final OpenAiChatModel openAiChatModel;

    public RetrievalInfoService(EmbeddingStore<TextSegment> embeddingStore, 
                                ChatService chatService, 
                                RetrievalInfoRepository repository) {
        this.embeddingStore = embeddingStore;
        this.chatService = chatService;
        this.repository = repository;
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
                .apiKey(API_KEY)
                .modelName(MODEL_EMBEDDING_TEXT)
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
                        match.embedded().text(),
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

        saveRetrievalInfo(new RetrievalInfoSaveRequest(
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

    @Transactional
    public void saveRetrievalInfo(RetrievalInfoSaveRequest request) {
        Log.infof("Saving RetrievalInfo for ChatMessage ID: %s", request.chatMessageId());
        try {
            var chatMessage = repository.findByChatMessageId(request.chatMessageId());
            if (chatMessage == null) {
                throw new NotFoundException("ChatMessage not found with id: " + request.chatMessageId());
            }

            // Check if RetrievalInfo already exists
            if (chatMessage.retrievalInfo != null) {
                Log.warnf("RetrievalInfo already exists for ChatMessage ID: %s", request.chatMessageId());
                return;
            }

            RetrievalInfo retrievalInfo = new RetrievalInfo();
            retrievalInfo.userQuestion = request.userQuestion();

            // Set bidirectional relationship correctly
            List<HypoteticalQuestion> questions = request.hypoteticalQuestions().stream().map(question -> {
                HypoteticalQuestion hq = new HypoteticalQuestion();
                hq.question = question.question();
                hq.similarityScore = question.similarity().toString();
                hq.chunk = question.chunk();
                hq.retrievalInfo = retrievalInfo; // Set the parent reference
                return hq;
            }).toList();

            retrievalInfo.hypoteticalQuestions = questions;
            chatMessage.retrievalInfo = retrievalInfo;

            // Just set the relationship - cascade will handle persistence
            repository.persist(chatMessage);

            Log.infof("RetrievalInfo saved successfully for ChatMessage ID: %s with %d questions",
                     request.chatMessageId(), questions.size());
        } catch (Exception e) {
            Log.errorf(e, "Error saving RetrievalInfo for ChatMessage ID: %s", request.chatMessageId());
            throw e;
        }
    }

    public RetrievalInfo getRetrievalInfoByChatMessageId(String chatMessageId) {
        Log.infof("Fetching RetrievalInfo for ChatMessage ID: %s", chatMessageId);
        var chatMessage = repository.findByChatMessageId(chatMessageId);
        if (chatMessage == null || chatMessage.retrievalInfo == null) {
            throw new NotFoundException("RetrievalInfo not found for ChatMessage ID: " + chatMessageId);
        }
        return chatMessage.retrievalInfo;
    }

    @Transactional
    public void retrieveAndSaveInfo(String chatMessageId) {
        Log.infof("Retrieving and saving info for ChatMessage ID: %s", chatMessageId);

        var chatMessage = repository.findByChatMessageId(chatMessageId);
        if (chatMessage == null) {
            throw new NotFoundException("ChatMessage not found with id: " + chatMessageId);
        }

        if (chatMessage.content == null || chatMessage.content.trim().isEmpty()) {
            throw new IllegalArgumentException("ChatMessage content is empty for id: " + chatMessageId);
        }

        String userQuestion = chatMessage.content;
        Log.infof("User question from message: %s", userQuestion);

        List<Question> questions = retrieveQuestions(userQuestion);
        Log.infof("Retrieved %d questions for messageId=%s",
                 questions != null ? questions.size() : 0, chatMessageId);

        if (questions == null || questions.isEmpty()) {
            Log.warnf("No questions retrieved for ChatMessage ID: %s", chatMessageId);
            throw new NotFoundException("No relevant content found for this question");
        }

        saveRetrievalInfo(
                new RetrievalInfoSaveRequest(
                        chatMessageId,
                        userQuestion,
                        questions
                )
        );
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

    public void ingestionOfHypotheticalQuestions(byte[] docBytes, String fileName) {
        Log.infof("Ingesting hypothetical questions for document: %s", fileName);

        Document document = toDocument(docBytes);
        List<QuestionParagraph> allQuestionParagraphs = new ArrayList<>();

        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(2000, 100);
        List<TextSegment> paragraphs = splitter.split(document);

        for (TextSegment paragraphSegment : paragraphs) {
            Log.infof("Processing paragraph: %s for document %s", paragraphSegment.text(), fileName);

            ChatResponse aiResult = openAiChatModel.chat(List.of(
                    SystemMessage.from("""
                            Suggest 2 clear questions whose answer could be given by the user provided text.
                            Don't use pronouns, be explicit about the subjects and objects of the question.
                            Return a JSON array of strings with the questions.
                            """),
                    UserMessage.from(paragraphSegment.text())
            ));

            String[] questions = toCleanedQuestions(aiResult.aiMessage().text());
            
            for (String question : questions) {
                Log.infof("Generated question: %s", question);
                allQuestionParagraphs.add(new QuestionParagraph(question, paragraphSegment));
            }
        }

        List<TextSegment> embeddedSegments = allQuestionParagraphs.stream()
                .map(questionParagraph -> TextSegment.from(
                        questionParagraph.question(),
                        new Metadata().put(PARAGRAPH_KEY, questionParagraph.paragraph().text())))
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(embeddedSegments).content();
        embeddingStore.addAll(embeddings, embeddedSegments);

        Log.infof("Successfully ingested %d hypothetical questions for document %s", allQuestionParagraphs.size(), fileName);
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