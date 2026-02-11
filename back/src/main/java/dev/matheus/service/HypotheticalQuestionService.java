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
import jakarta.enterprise.context.control.ActivateRequestContext;
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
     * Self-injection to call @Transactional methods through the CDI proxy.
     * Direct self-invocation (this.method()) bypasses CDI interceptors,
     * making @Transactional annotations NO-OPs.
     */
    @Inject
    HypotheticalQuestionService self;

    /**
     * Generate embeddings for all chunks in a document in parallel.
     * Each chunk is processed in its own transaction to ensure that failures
     * in one chunk don't rollback successful embeddings from other chunks.
     * Chunks are processed in parallel using CompletableFuture for better performance.
     *
     * @ActivateRequestContext: called from async threads that lack CDI request context.
     * NO @Transactional: loads data via separate method, then processes without transaction.
     */
    @ActivateRequestContext
    public void generateEmbeddings(String docId) {
        // Load document and chunks in a separate short transaction
        // Called via self (CDI proxy) so @Transactional is properly activated
        DocumentAndChunks data = self.loadDocumentAndChunks(docId);
        
        // Transaction committed in loadDocumentAndChunks() - no longer active here
        Log.infof("Starting embedding generation - docId=%s, fileName=%s", data.doc.id, data.doc.fileName);
        Log.infof("Found chunks for embedding - docId=%s, chunkCount=%d", data.doc.id, data.chunks.size());
        
        // Process in parallel - NO transaction held during this
        generateEmbeddingsParallel(data.doc, data.chunks);
    }
    
    /**
     * Load document and chunks in a short transaction.
     * Transaction commits as soon as this method returns.
     *
     * Uses JOIN FETCH to eagerly load chunk.documentFile, preventing
     * LazyInitializationException when chunks are processed on parallel
     * threads after this transaction closes (entities become detached).
     */
    @Transactional
    public DocumentAndChunks loadDocumentAndChunks(String docId) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        // JOIN FETCH ensures documentFile is eagerly loaded in a single query,
        // so chunk.documentFile.fileName is available after the entity detaches
        List<DocumentChunk> chunks = DocumentChunk
                .find("SELECT c FROM DocumentChunk c JOIN FETCH c.documentFile WHERE c.documentFile.id = ?1", doc.id)
                .list();
        return new DocumentAndChunks(doc, chunks);
    }
    
    /**
     * Helper class to hold document and chunks loaded from database.
     * Package-private so CDI proxy subclass can access the return type.
     */
    static class DocumentAndChunks {
        final DocumentFile doc;
        final List<DocumentChunk> chunks;
        
        DocumentAndChunks(DocumentFile doc, List<DocumentChunk> chunks) {
            this.doc = doc;
            this.chunks = chunks;
        }
    }
    
    /**
     * Perform parallel embedding generation for chunks.
     * Called after transaction commits to release database connection.
     * No @Transactional - each chunk/question has its own transaction.
     */
    private void generateEmbeddingsParallel(DocumentFile doc, List<DocumentChunk> chunks) {
        // Process chunks in parallel using CompletableFuture
        List<CompletableFuture<EmbeddingResult>> futures = chunks.stream()
            .map(chunk -> CompletableFuture.supplyAsync(
                () -> processChunkEmbedding(chunk),
                executorService
            ))
            .toList();
        
        // Wait for all tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        try {
            allOf.join();
            
            // Collect results
            int successCount = 0;
            int failureCount = 0;
            
            for (CompletableFuture<EmbeddingResult> future : futures) {
                EmbeddingResult result = future.join();
                if (result.success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }
            
            Log.infof("Embedding generation complete - docId=%s, total=%d, success=%d, failed=%d", 
                     doc.id, chunks.size(), successCount, failureCount);
        } catch (Exception e) {
            Log.errorf(e, "Error during parallel embedding generation - docId=%s", doc.id);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
    
    /**
     * Process embedding for a single chunk within a separate thread.
     * 1. Embeds content in a short transaction
     * 2. After transaction commits, embeds questions in parallel (each in own transaction)
     * This prevents holding a transaction open while waiting for parallel tasks.
     */
    private EmbeddingResult processChunkEmbedding(DocumentChunk chunk) {
        try {
            // Step 1: Embed content and generate questions (in transaction - fast)
            // Called via self (CDI proxy) so @Transactional and @ActivateRequestContext work
            // on this executor thread which has no CDI context
            List<String> questions = self.generateEmbeddingsInTransaction(chunk);
            
            // Transaction committed here - no longer holding DB connection
            
            // Step 2: Embed each question in parallel (each gets own transaction)
            if (!questions.isEmpty()) {
                embedQuestionsInParallel(chunk, questions);
            }
            
            Log.debugf("Embeddings completed - chunkId=%s, totalEmbeddings=%d", 
                      chunk.id, questions.size() + 1);
            
            return new EmbeddingResult(true, chunk.id, null);
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate embeddings for chunk - chunkId=%s, position=%d", 
                      chunk.id, chunk.position);
            return new EmbeddingResult(false, chunk.id, e.getMessage());
        }
    }
    
    /**
     * Embed questions sequentially, each with its own transaction.
     * Sequential processing avoids thread pool deadlock since chunks are already parallel.
     * No transaction context - each question creates its own transaction.
     */
    private void embedQuestionsInParallel(DocumentChunk chunk, List<String> questions) {
        // Process questions sequentially to avoid thread pool exhaustion/deadlock
        // Chunks are already being processed in parallel, so this is efficient
        for (String question : questions) {
            embedAndPersistQuestion(chunk, question);
        }
        
        Log.debugf("Completed question embeddings - chunkId=%s, questionCount=%d", 
                  chunk.id, questions.size());
    }
    
    /**
     * Result holder for parallel embedding processing
     */
    private static class EmbeddingResult {
        final boolean success;
        final String chunkId;
        final String errorMessage;
        
        EmbeddingResult(boolean success, String chunkId, String errorMessage) {
            this.success = success;
            this.chunkId = chunkId;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Generate embeddings for a single chunk within a transaction.
     * Only embeds the chunk content (fast). Questions are embedded after transaction completes.
     *
     * @ActivateRequestContext: runs on executor threads without CDI context.
     * Must execute BEFORE @Transactional (lower priority interceptor) so the
     * request-scoped EntityManager is available when the transaction starts.
     *
     * @param chunk The chunk to generate embeddings for
     * @return List of generated questions to embed (outside transaction)
     */
    @ActivateRequestContext
    @Transactional
    public List<String> generateEmbeddingsInTransaction(DocumentChunk chunk) {
        return embedContentAndGenerateQuestions(chunk);
    }
    
    /**
     * Embed chunk content and generate questions (but don't embed questions yet).
     * This keeps the transaction short - only for content embedding.
     * Questions will be embedded in parallel after this transaction commits.
     */
    private List<String> embedContentAndGenerateQuestions(DocumentChunk chunk) {
        Log.debugf("Generating embeddings for chunk - chunkId=%s, position=%d, tokens=%d", 
                  chunk.id, chunk.position, chunk.tokenCount);
        
        // Skip embedding for chunks that exceed the embedding model's context limit
        final int MAX_EMBEDDING_TOKENS = 7500;
        
        if (chunk.tokenCount > MAX_EMBEDDING_TOKENS) {
            Log.errorf("Skipping embedding for oversized chunk - chunkId=%s, tokens=%d (max=%d)",
                      chunk.id, chunk.tokenCount, MAX_EMBEDDING_TOKENS);
            return List.of(); // Skip this chunk entirely
        }
        
        // 1. Embed chunk content (in this transaction)
        TextSegment contentSegment = TextSegment.from(chunk.content,
            new Metadata()
                .put("FILE_NAME", chunk.documentFile.fileName)
                .put("CHUNK_ID", chunk.id)
                .put("POSITION", chunk.position));
        
        try {
            Embedding contentEmbedding = embeddingModel.embed(contentSegment).content();
            String contentEmbeddingId = embeddingStore.add(contentEmbedding, contentSegment);
            
            // Create ChunkEmbedding link
            ChunkEmbedding chunkEmbContent = new ChunkEmbedding();
            chunkEmbContent.chunk = chunk;
            chunkEmbContent.embeddingId = contentEmbeddingId;
            chunkEmbContent.embeddingType = "CONTENT";
            chunkEmbContent.persist();
        } catch (Exception e) {
            Log.errorf(e, "Failed to embed chunk content - chunkId=%s, contentLength=%d, tokens=%d", 
                      chunk.id, chunk.content.length(), chunk.tokenCount);
            return List.of(); // Skip questions if content fails
        }
        
        // 2. Generate hypothetical questions (but don't embed yet)
        List<String> questions = generateQuestions(chunk);
        Log.debugf("Generated questions for chunk - chunkId=%s, questionCount=%d", chunk.id, questions.size());
        
        // Return questions to be embedded AFTER transaction commits
        return questions;
    }
    
    /**
     * Embed a hypothetical question and persist it within a separate transaction.
     * Called via self (CDI proxy) so @Transactional and @ActivateRequestContext
     * are properly activated on the executor thread.
     */
    private void embedAndPersistQuestion(DocumentChunk chunk, String question) {
        self.embedAndPersistQuestionInTransaction(chunk, question);
    }
    
    /**
     * Embed and persist a question within a transaction.
     *
     * @ActivateRequestContext: runs on executor threads without CDI context.
     */
    @ActivateRequestContext
    @Transactional
    public void embedAndPersistQuestionInTransaction(DocumentChunk chunk, String question) {
        try {
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
        } catch (Exception e) {
            Log.errorf(e, "Failed to embed question for chunk - chunkId=%s, question=%s", 
                      chunk.id, question.substring(0, Math.min(50, question.length())));
            // Continue - failure to embed one question doesn't stop the process
        }
    }

    /**
     * Generate hypothetical questions for a chunk
     */
    public List<String> generateQuestions(DocumentChunk chunk) {
        // Skip question generation for chunks that are too large
        // Max token limit is 8192, but we need room for system message (~150 tokens)
        // and response (~500 tokens). Safe limit: 7500 tokens for content
        final int MAX_CONTENT_TOKENS = 7500;
        
        if (chunk.tokenCount > MAX_CONTENT_TOKENS) {
            Log.warnf("Skipping question generation for oversized chunk - chunkId=%s, tokens=%d (max=%d)",
                     chunk.id, chunk.tokenCount, MAX_CONTENT_TOKENS);
            return List.of(); // Return empty list, will only embed content
        }
        
        try {
            // Use existing AI service
            if (chunk.contentType == ContentType.TABLE) {
                return questionExtractorAiService.extractQuestionsFromTable(chunk.content);
            } else {
                return questionExtractorAiService.extractQuestions(chunk.content);
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to generate questions for chunk - chunkId=%s, contentLength=%d", 
                      chunk.id, chunk.content.length());
            // Return empty list on error - chunk will still have content embedding
            return List.of();
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
