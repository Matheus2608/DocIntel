package dev.matheus.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import dev.matheus.entity.ProcessingStatus;
import dev.matheus.service.docling.DoclingDocumentParser;
import dev.matheus.service.pdf.PdfTableExtractor;
import dev.matheus.service.pdf.PdfTextExtractor;
import dev.matheus.service.pdf.TextNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for ingesting and parsing documents, especially PDFs.
 */
@ApplicationScoped
public class DocumentIngestionService {

    private static final Logger Log = Logger.getLogger(DocumentIngestionService.class);

    @Inject
    DoclingDocumentParser doclingParser;

    /**
     * Self-injection to call @Transactional methods through the CDI proxy.
     * Direct self-invocation (this.method()) bypasses CDI interceptors,
     * making @Transactional annotations NO-OPs.
     */
    @Inject
    DocumentIngestionService self;

    /**
     * Process document using Docling and persist chunks.
     *
     * NOT @Transactional: the Docling API call can take hours. Holding a single
     * transaction open would cause JTA timeout (1h) before Docling finishes (2h).
     * Instead, uses short focused transactions for each database operation.
     *
     * @ActivateRequestContext: this method runs on async threads (from
     * AsyncDocumentProcessingService) that have no CDI request context.
     * The EntityManager is request-scoped and needs an active context.
     */
    @ActivateRequestContext
    public void processDocument(String docId) {
        Log.infof("Starting document processing - docId=%s", docId);

        try {
            // Short transaction: load document and set PROCESSING status
            DocumentFile doc = self.startProcessing(docId);

            // NO transaction: external HTTP call to Docling (can take hours)
            Log.debugf("Parsing document with Docling - docId=%s, fileName=%s", docId, doc.fileName);
            List<DocumentChunk> chunks = doclingParser.parse(doc, doc.fileData);
            Log.infof("Docling parsing complete - docId=%s, chunks=%d", docId, chunks.size());

            // Short transaction: persist chunks and mark COMPLETED
            self.persistChunksAndComplete(docId, chunks);
            Log.infof("Document processing completed successfully - docId=%s, chunks=%d", docId, chunks.size());

        } catch (Exception e) {
            Log.errorf(e, "Document processing failed - docId=%s, error=%s", docId, e.getMessage());
            try {
                // Separate transaction: mark as FAILED
                // Called via self (CDI proxy) so @Transactional is activated.
                // Since processDocument() has no @Transactional, the RuntimeException
                // below does NOT trigger a rollback — the FAILED status is safe.
                self.markAsFailed(docId, e.getMessage());
            } catch (Exception failEx) {
                Log.errorf(failEx, "Failed to mark document as FAILED - docId=%s", docId);
            }
            throw new RuntimeException("Document processing failed", e);
        }
    }

    /**
     * Transition document to PROCESSING status and return the entity.
     * Short transaction that commits immediately.
     * The returned entity will be detached after commit — its simple fields
     * (fileName, fileData) remain accessible in memory.
     */
    @Transactional
    public DocumentFile startProcessing(String docId) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        doc.processingStatus = ProcessingStatus.PROCESSING;
        doc.persist();
        return doc;
    }

    /**
     * Persist document chunks and mark processing as complete.
     * Short transaction for database writes only.
     */
    @Transactional
    public void persistChunksAndComplete(String docId, List<DocumentChunk> chunks) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }

        // Re-associate chunks with the managed entity (chunks reference a
        // detached DocumentFile from before the Docling call)
        for (DocumentChunk chunk : chunks) {
            chunk.documentFile = doc;
            chunk.persist();
        }

        doc.processingStatus = ProcessingStatus.COMPLETED;
        doc.processedAt = LocalDateTime.now();
        doc.chunkCount = chunks.size();
        doc.processorVersion = "docling-serve-v1.9.0";
        doc.persist();
    }

    /**
     * Mark document as COMPLETED (kept for external callers and tests)
     */
    @Transactional
    public void completeProcessing(String docId, List<DocumentChunk> chunks) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        doc.processingStatus = ProcessingStatus.COMPLETED;
        doc.processedAt = LocalDateTime.now();
        doc.chunkCount = chunks.size();
        doc.processorVersion = "docling-serve-v1.9.0";
        doc.persist();
    }

    /**
     * Mark document as FAILED with error message
     */
    @Transactional
    public void markAsFailed(String docId, String errorMessage) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            Log.errorf("Cannot mark as failed - document not found: docId=%s", docId);
            return;
        }
        doc.processingStatus = ProcessingStatus.FAILED;
        doc.processingError = errorMessage;
        doc.processedAt = LocalDateTime.now();
        doc.persist();
    }

    /**
     * Parses a PDF document, extracts clean text and tables in Markdown format.
     * 
     * @deprecated Use {@link #processDocument(DocumentFile)} instead for Docling-based processing.
     *             This method uses legacy PDFBox/Tabula extraction and will be removed in a future version.
     *             Migration: Replace parseCustomPdf() calls with processDocument() to benefit from
     *             Docling's superior table extraction and semantic chunking.
     */
    @Deprecated(since = "Phase 9", forRemoval = true)
    public Document parseCustomPdf(byte[] pdfBytes, String filename) throws IOException {
        long startTime = System.currentTimeMillis();
        Log.infof("Starting PDF parsing - filename=%s, size=%d bytes", filename, pdfBytes.length);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            Log.infof("PDF loaded successfully - pages=%d", pageCount);

            // Extract table regions and tables in a single pass to avoid PageIterator exhaustion
            Log.debug("Extracting table regions and tables");
            PdfTableExtractor.ExtractionResult extractionResult = PdfTableExtractor.extractTablesAndRegions(document);
            Log.infof("Tables extracted - found %d tables",
                    extractionResult.tablesMarkdown.split("\\[START_TABLE\\]").length - 1);

            // Extract text with table markers - must be done while document is still open
            Log.debug("Extracting text with table markers");
            PdfTextExtractor textExtractor = new PdfTextExtractor(extractionResult.tableRegions);
            String textWithTags = textExtractor.getText(document);
            Log.debugf("Text extracted - length=%d chars", textWithTags.length());

            // Now we can safely process the extracted strings after getting all data from document
            // Clean the text
            Log.debug("Normalizing text");
            String cleanText = TextNormalizer.normalize(textWithTags);

            // Combine text and tables
            String finalContent = cleanText + "\n\n" + extractionResult.tablesMarkdown;

            long duration = System.currentTimeMillis() - startTime;
            Log.infof("PDF parsing completed in %dms - filename=%s, finalLength=%d chars",
                    duration, filename, finalContent.length());

            return Document.from(finalContent, new Metadata().put("FILE_NAME", filename));
        } catch (Exception ex) {
            Log.errorf(ex, "PDF parsing failed - filename=%s", filename);
            throw ex;
        }
    }
}