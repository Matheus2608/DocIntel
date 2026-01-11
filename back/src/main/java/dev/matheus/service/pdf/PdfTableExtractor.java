package dev.matheus.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting table regions from PDF documents using Tabula.
 */
public class PdfTableExtractor {

    private PdfTableExtractor() {
        // Utility class
    }

    /**
     * Result containing both table regions and extracted tables as Markdown
     */
    public static class ExtractionResult {
        public final Map<Integer, List<Rectangle>> tableRegions;
        public final String tablesMarkdown;

        public ExtractionResult(Map<Integer, List<Rectangle>> tableRegions, String tablesMarkdown) {
            this.tableRegions = tableRegions;
            this.tablesMarkdown = tablesMarkdown;
        }
    }

    /**
     * Extracts both table regions and table content in a single pass.
     * This avoids the issue of consuming the PageIterator twice.
     * Note: Does NOT close the ObjectExtractor - caller must manage document lifecycle.
     */
    public static ExtractionResult extractTablesAndRegions(PDDocument document) throws IOException {
        Map<Integer, List<Rectangle>> tableRegionsByPage = new HashMap<>();
        StringBuilder allTables = new StringBuilder();

        ObjectExtractor extractor = new ObjectExtractor(document);
        SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();
        PageIterator pages = extractor.extract();

        while (pages.hasNext()) {
            Page page = pages.next();
            if (page == null) {
                continue;
            }

            List<Table> tables = algorithm.extract(page);

            for (Table table : tables) {
                // Store table regions
                tableRegionsByPage.computeIfAbsent(page.getPageNumber(), k -> new ArrayList<>()).add(table);

                // Extract table as Markdown
                String markdown = TableToMarkdownConverter.convert(table);
                if (!markdown.isBlank()) {
                    allTables.append(markdown);
                }
            }
        }

        return new ExtractionResult(tableRegionsByPage, allTables.toString());
    }
}

