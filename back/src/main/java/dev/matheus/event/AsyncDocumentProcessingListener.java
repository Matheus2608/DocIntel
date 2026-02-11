package dev.matheus.event;

import dev.matheus.service.AsyncDocumentProcessingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Listener for DocumentCreatedEvent that triggers async processing
 * AFTER the transaction commits successfully.
 * 
 * This ensures the document is visible to other threads/transactions
 * before async processing attempts to find it in the database.
 */
@ApplicationScoped
public class AsyncDocumentProcessingListener {
    
    private static final Logger LOG = Logger.getLogger(AsyncDocumentProcessingListener.class);
    
    @Inject
    AsyncDocumentProcessingService asyncProcessingService;
    
    /**
     * Handle document creation event AFTER transaction commits.
     * 
     * The @Observes annotation with TransactionPhase.AFTER_SUCCESS ensures
     * this method is called only after the transaction commits successfully.
     * 
     * This solves the race condition where async processing tried to find
     * the document before it was visible in the database.
     */
    public void onDocumentCreated(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) DocumentCreatedEvent event) {
        
        LOG.infof("Transaction committed successfully, starting async processing: docId=%s, fileName=%s",
                event.getDocumentId(), event.getFileName());
        
        asyncProcessingService.processDocumentAsync(event.getDocumentId(), event.getFileName());
    }
}
