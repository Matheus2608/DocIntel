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
     * Process document using Docling and persist chunks
     */
    @Transactional
    public void processDocument(String docId) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        
        Log.infof("Starting document processing - docId=%s, fileName=%s", doc.id, doc.fileName);
        try {
            // Transition to PROCESSING
            startProcessing(docId);
            
            // Parse with Docling
            Log.debugf("Parsing document with Docling - docId=%s", doc.id);
            List<DocumentChunk> chunks = doclingParser.parse(doc, doc.fileData);
            Log.infof("Docling parsing complete - docId=%s, chunks=%d", doc.id, chunks.size());
            
            // Persist chunks
            for (DocumentChunk chunk : chunks) {
                chunk.persist();
            }
            
            // Complete processing
            completeProcessing(docId, chunks);
            Log.infof("Document processing completed successfully - docId=%s, chunks=%d", doc.id, chunks.size());
            
        } catch (Exception e) {
            Log.errorf(e, "Document processing failed - docId=%s, error=%s", docId, e.getMessage());
            markAsFailed(docId, e.getMessage());
            throw new RuntimeException("Document processing failed", e);
        }
    }

    /**
     * Transition document to PROCESSING status
     */
    @Transactional
    public void startProcessing(String docId) {
        DocumentFile doc = DocumentFile.findById(docId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not found: " + docId);
        }
        doc.processingStatus = ProcessingStatus.PROCESSING;
        doc.persist();
    }

    /**
     * Mark document as COMPLETED
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