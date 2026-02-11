package dev.matheus.service;

import dev.matheus.entity.DocumentFile;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for asynchronous document processing.
 * Handles long-running Docling parsing and embedding generation without blocking HTTP requests.
 */
@ApplicationScoped
public class AsyncDocumentProcessingService {

    private static final Logger LOG = Logger.getLogger(AsyncDocumentProcessingService.class);
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final Map<String, CompletableFuture<Void>> processingTasks = new ConcurrentHashMap<>();
    
    @Inject
    DocumentIngestionService documentIngestionService;
    
    @Inject
    HypotheticalQuestionService hypotheticalQuestionService;
    
    /**
     * Start asynchronous processing for a document.
     * Returns immediately while processing continues in background.
     */
    public void processDocumentAsync(String docId, String fileName) {
        LOG.infof("Queuing document for async processing: docId=%s, fileName=%s", 
                docId, fileName);
        
        // Check if already processing
        if (processingTasks.containsKey(docId)) {
            LOG.warnf("Document is already being processed: docId=%s", docId);
            return;
        }
        
        // Submit processing task
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                LOG.infof("Starting async processing: docId=%s", docId);
                
                // Step 1: Parse with Docling and create chunks
                documentIngestionService.processDocument(docId);
                
                // Step 2: Generate embeddings
                hypotheticalQuestionService.generateEmbeddings(docId);
                
                LOG.infof("Async processing completed successfully: docId=%s", docId);
                
            } catch (Exception e) {
                LOG.errorf(e, "Async processing failed: docId=%s", docId);
                // Document status is marked as FAILED by documentIngestionService.processDocument()
                // via self.markAsFailed() in a separate committed transaction
            } finally {
                // Remove from tracking map
                processingTasks.remove(docId);
            }
        }, executorService);
        
        processingTasks.put(docId, future);
    }
    
    /**
     * Check if a document is currently being processed.
     */
    public boolean isProcessing(String documentId) {
        return processingTasks.containsKey(documentId);
    }
    
    /**
     * Get the number of documents currently being processed.
     */
    public int getProcessingCount() {
        return processingTasks.size();
    }

    /**
     * Gracefully shut down the executor service on application stop.
     */
    @PreDestroy
    void shutdown() {
        LOG.info("Shutting down async document processing executor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(240, TimeUnit.SECONDS)) {
                LOG.warn("Executor did not terminate in 240s, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
