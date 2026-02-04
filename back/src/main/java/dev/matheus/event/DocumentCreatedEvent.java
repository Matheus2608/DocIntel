package dev.matheus.event;

/**
 * Event fired when a document is created and ready for async processing.
 * This event is fired AFTER the transaction commits to ensure data visibility.
 */
public class DocumentCreatedEvent {
    
    private final String documentId;
    private final String fileName;
    
    public DocumentCreatedEvent(String documentId, String fileName) {
        this.documentId = documentId;
        this.fileName = fileName;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public String getFileName() {
        return fileName;
    }
}
