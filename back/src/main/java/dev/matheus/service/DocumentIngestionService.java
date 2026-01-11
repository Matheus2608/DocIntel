package dev.matheus.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.matheus.service.pdf.PdfTableExtractor;
import dev.matheus.service.pdf.PdfTextExtractor;
import dev.matheus.service.pdf.TextNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Service responsible for ingesting and parsing documents, especially PDFs.
 */
@ApplicationScoped
public class DocumentIngestionService {

    private static final Logger Log = Logger.getLogger(DocumentIngestionService.class);
    /**
     * Parses a PDF document, extracts clean text and tables in Markdown format.
     */
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