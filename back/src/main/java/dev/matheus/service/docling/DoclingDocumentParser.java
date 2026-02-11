package dev.matheus.service.docling;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.options.TableFormerMode;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.request.target.InBodyTarget;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for parsing documents using Docling.
 * Extracts structured content including tables in markdown format.
 */
@ApplicationScoped
public class DoclingDocumentParser {

    private static final Logger LOG = Logger.getLogger(DoclingDocumentParser.class);

    @Inject
    DoclingServeApi doclingServeApi;

    @Inject
    DoclingConfigProperties config;

    private final ContentTypeDetector contentTypeDetector = new ContentTypeDetector();
    private final TokenEstimator tokenEstimator = new TokenEstimator();

    /**
     * Parse a document and extract chunks with structured content.
     * Supports PDF, DOCX, and DOC formats via Docling conversion.
     *
     * @param documentFile The document file entity
     * @param documentContent The document content as byte array
     * @return List of document chunks with extracted content
     * @throws RuntimeException if parsing fails
     */
    public List<DocumentChunk> parse(DocumentFile documentFile, byte[] documentContent) {
        LOG.infof("Parsing document: %s", documentFile.fileName);

        if (documentContent == null || documentContent.length == 0) {
            LOG.warn("Empty or null document content provided");
            return List.of();
        }

        // Check for minimal/empty documents that Docling cannot process
        if (isMinimalOrEmptyDocument(documentContent)) {
            LOG.warnf("Document %s appears to be empty or minimal, skipping", documentFile.fileName);
            return List.of();
        }

        try {
            // Call Docling Serve API to convert document to markdown
            String markdownContent = callDoclingApi(documentContent, documentFile.fileName);

            // Extract chunks from markdown content
            List<DocumentChunk> chunks = extractChunks(markdownContent, documentFile);

            LOG.infof("Successfully parsed document %s into %d chunks", documentFile.fileName, chunks.size());
            return chunks;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse document: %s", documentFile.fileName);
            throw new RuntimeException("Failed to parse document: " + e.getMessage(), e);
        }
    }

    /**
     * Check if document content is minimal or empty.
     * Validates that the document has sufficient content for processing.
     * 
     * @param documentContent The raw document bytes
     * @return true if document is too minimal to process
     */
    private boolean isMinimalOrEmptyDocument(byte[] documentContent) {
        // Check minimum content length (50 bytes as heuristic)
        // Most valid documents have headers, metadata, and content structure
        final int MIN_DOCUMENT_SIZE = 50;
        if (documentContent.length < MIN_DOCUMENT_SIZE) {
            return true;
        }
        
        // Check for empty PDF marker (minimal valid PDF with no content)
        String contentStr = new String(documentContent);
        if (contentStr.trim().equals("%PDF-1.4\n%%EOF")) {
            return true;
        }
        
        return false;
    }

    /**
     * Call Docling Serve API to convert document to markdown.
     * Supports PDF, DOCX, and DOC formats.
     * IMPORTANT: This method must be called from a worker thread (not event loop).
     * The async processing ensures this happens via AsyncDocumentProcessingService.
     * 
     * @param documentContent The raw document bytes
     * @param fileName The document filename (used for format detection)
     * @return Markdown representation of the document
     * @throws RuntimeException if conversion fails
     */
    private String callDoclingApi(byte[] documentContent, String fileName) {
        LOG.infof("Converting document to markdown via Docling API: %s", fileName);

        try {
            // Encode document content to Base64 (required by FileSource)
            String base64Content = Base64.getEncoder().encodeToString(documentContent);

            ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                    .source(FileSource.builder()
                            .base64String(base64Content)
                            .filename(fileName)
                            .build())
                    .options(ConvertDocumentOptions.builder()
                            .toFormat(OutputFormat.MARKDOWN)
                            .tableMode(TableFormerMode.ACCURATE) // Use accurate table extraction
                            .includeImages(false) // Skip images for now (focus on text/tables)
                            .abortOnError(false) // Continue on partial errors
                            .build())
                    .target(InBodyTarget.builder().build()) // Get results in HTTP response body
                    .build();

            ConvertDocumentResponse response = doclingServeApi.convertSource(request);
            
            if (response == null || response.getDocument() == null) {
                throw new RuntimeException("Docling API returned null response");
            }

            String markdown = response.getDocument().getMarkdownContent();
            
            if (markdown == null || markdown.isEmpty()) {
                LOG.warnf("Docling API returned empty markdown for: %s", fileName);
                return "";
            }

            LOG.infof("Docling API successfully converted %s to %d characters of markdown", 
                      fileName, markdown.length());
            
            return markdown;
            
        } catch (Exception e) {
            LOG.errorf(e, "Docling API call failed for: %s - %s", fileName, e.getClass().getSimpleName());
            throw new RuntimeException("Docling API conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract chunks from markdown content.
     * Uses table-aware chunking to preserve semantic structure.
     * 
     * @param markdownContent The markdown content to chunk
     * @param documentFile The document file entity for chunk association
     * @return List of document chunks with metadata
     */
    private List<DocumentChunk> extractChunks(String markdownContent, DocumentFile documentFile) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            LOG.warn("No content extracted from document");
            return chunks;
        }

        // Split content into semantic chunks using table-aware chunker
        MarkdownTableChunker chunker = new MarkdownTableChunker(
            config.chunking().maxTokens(), 
            tokenEstimator
        );
        List<String> contentChunks = chunker.split(markdownContent);
        
        AtomicInteger position = new AtomicInteger(0);
        
        for (String content : contentChunks) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.documentFile = documentFile;
            chunk.content = content;
            chunk.position = position.getAndIncrement();
            chunk.contentType = contentTypeDetector.detect(content);
            chunk.tokenCount = tokenEstimator.estimate(content);
            
            chunks.add(chunk);
        }

        return chunks;
    }
}
