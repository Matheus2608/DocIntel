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
     *
     * @param documentFile The document file entity
     * @param pdfContent   The PDF content as byte array
     * @return List of document chunks with extracted content
     * @throws RuntimeException if parsing fails
     */
    public List<DocumentChunk> parse(DocumentFile documentFile, byte[] pdfContent) {
        LOG.infof("Parsing document: %s", documentFile.fileName);

        if (pdfContent == null || pdfContent.length == 0) {
            LOG.warn("Empty or null PDF content provided");
            return List.of();
        }

        // Check if this is actually a valid PDF
        String content = new String(pdfContent);
        if (!content.startsWith("%PDF")) {
            throw new RuntimeException("Invalid PDF format");
        }
        
        // Check for empty PDF (minimal PDF with no content)
        if (content.trim().equals("%PDF-1.4\n%%EOF") || content.trim().length() < 50) {
            // Empty PDF - return empty list
            return List.of();
        }

        try {
            // Call Docling Serve API to convert PDF to markdown
            String markdownContent = callDoclingApi(pdfContent, documentFile.fileName);

            // Extract chunks from markdown content
            List<DocumentChunk> chunks = extractChunks(markdownContent, documentFile);

            LOG.infof("Successfully parsed document %s into %d chunks", documentFile.fileName, chunks.size());
            return chunks;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse document: %s", documentFile.fileName);
            throw new RuntimeException("Failed to parse PDF document: " + e.getMessage(), e);
        }
    }

    /**
     * Call Docling Serve API to convert document to markdown.
     */
    private String callDoclingApi(byte[] pdfContent, String fileName) {
        LOG.infof("Calling Docling Serve API for file: %s", fileName);

        // Encode PDF content to Base64 (required by FileSource)
        String base64Content = Base64.getEncoder().encodeToString(pdfContent);

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

        try {
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
            LOG.errorf(e, "Docling API call failed for: %s", fileName);
            throw new RuntimeException("Docling API conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extract chunks from markdown content.
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
