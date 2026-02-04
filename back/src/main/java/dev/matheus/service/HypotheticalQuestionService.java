package dev.matheus.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.matheus.ai.QuestionExtractorAiService;
import dev.matheus.entity.ChunkEmbedding;
import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import dev.matheus.splitter.CustomTableAwareSplitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static dev.matheus.util.ChatUtils.isPdf;
import static dev.matheus.util.ChatUtils.toDocument;

@ApplicationScoped
public class HypotheticalQuestionService {

    private static final Logger Log = Logger.getLogger(HypotheticalQuestionService.class);
    private static final String FILE_NAME_KEY = "FILE_NAME";
    private static final String PARAGRAPH_KEY = "PARAGRAPH";

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    DocumentIngestionService documentIngestionService;

    @Inject
    CustomTableAwareSplitter customTableAwareSplitter;

    @Inject
    QuestionExtractorAiService questionExtractorAiService;

    @Inject
    @Named("retrievalExecutorService")
    ExecutorService executorService;

    /**
     * Generate embeddings for all chunks in a document
     */
    @Transactional
    public void generateEmbeddings(String docId) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        
        Log.infof("Starting embedding generation - docId=%s, fileName=%s", doc.id, doc.fileName);
        List<DocumentChunk> chunks = DocumentChunk.list("documentFile.id", doc.id);
        Log.infof("Found chunks for embedding - docId=%s, chunkCount=%d", doc.id, chunks.size());
        
        for (DocumentChunk chunk : chunks) {
            generateEmbeddings(chunk);
        }
        
        Log.infof("Embedding generation complete - docId=%s, chunks=%d", doc.id, chunks.size());
    }

    /**
     * Generate embeddings for a single chunk
     * Creates both CONTENT and HYPOTHETICAL_QUESTION embeddings
     */
    @Transactional
    public void generateEmbeddings(DocumentChunk chunk) {
        Log.debugf("Generating embeddings for chunk - chunkId=%s, position=%d", chunk.id, chunk.position);
        
        // 1. Embed chunk content
        TextSegment contentSegment = TextSegment.from(chunk.content,
            new Metadata()
                .put("FILE_NAME", chunk.documentFile.fileName)
                .put("CHUNK_ID", chunk.id)
                .put("POSITION", chunk.position));
        
        Embedding contentEmbedding = embeddingModel.embed(contentSegment).content();
        String contentEmbeddingId = embeddingStore.add(contentEmbedding, contentSegment);
        
        // Create ChunkEmbedding link
        ChunkEmbedding chunkEmbContent = new ChunkEmbedding();
        chunkEmbContent.chunk = chunk;
        chunkEmbContent.embeddingId = contentEmbeddingId;
        chunkEmbContent.embeddingType = "CONTENT";
        chunkEmbContent.persist();
        
        // 2. Generate hypothetical questions
        List<String> questions = generateQuestions(chunk);
        Log.debugf("Generated questions for chunk - chunkId=%s, questionCount=%d", chunk.id, questions.size());
        
        // 3. Embed each question
        for (String question : questions) {
            TextSegment questionSegment = TextSegment.from(question,
                new Metadata()
                    .put("PARAGRAPH", chunk.content)
                    .put("FILE_NAME", chunk.documentFile.fileName)
                    .put("CHUNK_ID", chunk.id));
            
            Embedding questionEmbedding = embeddingModel.embed(questionSegment).content();
            String questionEmbeddingId = embeddingStore.add(questionEmbedding, questionSegment);
            
            // Link question embedding to chunk
            ChunkEmbedding chunkEmbQuestion = new ChunkEmbedding();
            chunkEmbQuestion.chunk = chunk;
            chunkEmbQuestion.embeddingId = questionEmbeddingId;
            chunkEmbQuestion.embeddingType = "HYPOTHETICAL_QUESTION";
            chunkEmbQuestion.persist();
        }
        
        Log.debugf("Embeddings persisted - chunkId=%s, totalEmbeddings=%d", chunk.id, questions.size() + 1);
    }

    /**
     * Generate hypothetical questions for a chunk
     */
    public List<String> generateQuestions(DocumentChunk chunk) {
        // Use existing AI service
        if (chunk.contentType == ContentType.TABLE) {
            return questionExtractorAiService.extractQuestionsFromTable(chunk.content);
        } else {
            return questionExtractorAiService.extractQuestions(chunk.content);
        }
    }

    /**
     * Legacy method for ingesting documents without persistence to database.
     * 
     * @deprecated Use {@link #generateEmbeddings(DocumentFile)} instead for database-backed embeddings.
     *             This method directly stores embeddings in the vector store without ChunkEmbedding links,
     *             making it impossible to trace embeddings back to source chunks.
     *             Migration: Use processDocument() + generateEmbeddings() for persistent, traceable embeddings.
     */
    @Deprecated(since = "Phase 9", forRemoval = true)
    public void ingestHypotheticalQuestions(byte[] docBytes, String fileName, String fileType) throws IOException {
        Log.infof("Ingesting hypothetical questions for document: %s", fileName);

        Document document;
        if (isPdf(fileType, fileName)) {
            document = documentIngestionService.parseCustomPdf(docBytes, fileName);
        } else {
            document = toDocument(docBytes, fileType, fileName);
        }

        var paragraphs = customTableAwareSplitter.split(document);

        List<TextSegment> embeddedSegments = getHypotheticalQuestionsAndParagraphsSegments(paragraphs, fileName);
        List<Embedding> embeddings = embeddingModel.embedAll(embeddedSegments).content();
        embeddingStore.addAll(embeddings, embeddedSegments);

        Log.infof("Successfully ingested %d segments for document %s", embeddedSegments.size(), fileName);
    }

    private List<TextSegment> getHypotheticalQuestionsAndParagraphsSegments(List<TextSegment> paragraphs, String fileName) {
        paragraphs.forEach(p -> p.metadata().put(FILE_NAME_KEY, fileName));

        List<QuestionParagraph> questionParagraphs = parallelProcessing(paragraphs, fileName);
        List<TextSegment> embeddedSegments = questionParagraphsToTextSegments(questionParagraphs, fileName);
        Log.infof("Successfully generated %d hypothetical questions for document %s", questionParagraphs.size(), fileName);

        embeddedSegments.addAll(paragraphs); // Also index the original paragraphs
        return embeddedSegments;
    }

    private List<QuestionParagraph> parallelProcessing(List<TextSegment> paragraphs, String fileName) {
        Log.infof("Starting parallel question generation - paragraphs=%d, fileName=%s",
                paragraphs.size(), fileName);

        List<CompletableFuture<List<QuestionParagraph>>> futures = paragraphs.stream()
                .map(paragraphSegment -> CompletableFuture.supplyAsync(() -> {
                    String text = paragraphSegment.text();
                    List<String> questions = text.startsWith("[START_TABLE]")
                            ? questionExtractorAiService.extractQuestionsFromTable(text)
                            : questionExtractorAiService.extractQuestions(text);

                    Log.debugf("Generated %d questions from paragraph", questions.size());

                    List<QuestionParagraph> paragraphQuestions = new ArrayList<>();
                    for (String question : questions) {
                        paragraphQuestions.add(new QuestionParagraph(question, paragraphSegment));
                    }

                    return paragraphQuestions;
                }, executorService))
                .toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        List<QuestionParagraph> result = new ArrayList<>();
        try {
            allOf.join();
            for (CompletableFuture<List<QuestionParagraph>> future : futures) {
                result.addAll(future.join());
            }
            Log.infof("Parallel processing completed - generated %d questions from %d paragraphs for fileName=%s",
                    result.size(), paragraphs.size(), fileName);
        } catch (Exception e) {
            Log.errorf(e, "Error during parallel question generation - fileName=%s, paragraphs=%d",
                    fileName, paragraphs.size());
            throw new RuntimeException("Failed to generate hypothetical questions", e);
        }

        return result;
    }

    private List<TextSegment> questionParagraphsToTextSegments(List<QuestionParagraph> questionParagraphs, String fileName) {
        return questionParagraphs.stream()
                .map(questionParagraph -> TextSegment.from(
                        questionParagraph.question(),
                        new Metadata()
                                .put(PARAGRAPH_KEY, questionParagraph.paragraph().text())
                                .put(FILE_NAME_KEY, fileName)
                ))
                .collect(Collectors.toList());
    }

    private record QuestionParagraph(String question, TextSegment paragraph) {
    }
}
