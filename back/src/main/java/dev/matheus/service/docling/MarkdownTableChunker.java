package dev.matheus.service.docling;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits markdown content into semantic chunks while preserving table boundaries.
 * Tables are kept together in single chunks to maintain semantic cohesion.
 */
public class MarkdownTableChunker {

    private static final Logger LOG = Logger.getLogger(MarkdownTableChunker.class);

    private final int maxTokensPerChunk;
    private final TokenEstimator tokenEstimator;

    public MarkdownTableChunker(int maxTokensPerChunk, TokenEstimator tokenEstimator) {
        this.maxTokensPerChunk = maxTokensPerChunk;
        this.tokenEstimator = tokenEstimator;
    }

    /**
     * Split markdown content into semantic chunks.
     * Tables are kept together in single chunks.
     *
     * @param content The markdown content to split
     * @return List of content chunks
     */
    public List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        
        ChunkBuilder currentChunk = new ChunkBuilder();
        TableBuilder currentTable = new TableBuilder();
        boolean inTable = false;
        
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (isTableLine(line)) {
                if (!inTable) {
                    // Starting new table - save current chunk if not empty
                    if (currentChunk.hasContent()) {
                        chunks.add(currentChunk.build());
                        currentChunk = new ChunkBuilder();
                    }
                    inTable = true;
                }
                currentTable.addLine(line);
            } else {
                if (inTable) {
                    // Table ended - check size and potentially split
                    String tableContent = currentTable.build();
                    int tableTokens = tokenEstimator.estimate(tableContent);
                    
                    if (tableTokens > maxTokensPerChunk) {
                        // Table is too large - split it by rows
                        LOG.warnf("Large table detected (%d tokens) - splitting into smaller chunks", tableTokens);
                        chunks.addAll(splitLargeTable(tableContent));
                    } else {
                        chunks.add(tableContent);
                    }
                    
                    currentTable = new TableBuilder();
                    inTable = false;
                }
                
                // Add line to regular content
                if (!line.trim().isEmpty()) {
                    currentChunk.addLine(line);
                    
                    // Create chunk if it's getting large
                    if (tokenEstimator.estimate(currentChunk.content()) > maxTokensPerChunk) {
                        chunks.add(currentChunk.build());
                        currentChunk = new ChunkBuilder();
                    }
                }
            }
        }
        
        // Save any remaining table
        if (inTable && currentTable.hasContent()) {
            String tableContent = currentTable.build();
            int tableTokens = tokenEstimator.estimate(tableContent);
            
            if (tableTokens > maxTokensPerChunk) {
                // Table is too large - split it by rows
                LOG.warnf("Large table detected (%d tokens) - splitting into smaller chunks", tableTokens);
                chunks.addAll(splitLargeTable(tableContent));
            } else {
                chunks.add(tableContent);
            }
        }
        
        // Save any remaining content
        if (currentChunk.hasContent()) {
            chunks.add(currentChunk.build());
        }
        
        LOG.debugf("Split markdown into %d chunks", chunks.size());
        return chunks;
    }

    private boolean isTableLine(String line) {
        return line.trim().split("|").length >= 3;
    }

    /**
     * Split a large table into smaller chunks by rows.
     * Preserves table header in each chunk.
     */
    private List<String> splitLargeTable(String tableContent) {
        List<String> chunks = new ArrayList<>();
        String[] lines = tableContent.split("\n");
        
        if (lines.length < 3) {
            // Too small to split (header + separator + at least 1 row)
            chunks.add(tableContent);
            return chunks;
        }
        
        // Extract header (first 2 lines: header + separator)
        String header = lines[0] + "\n" + lines[1];
        int headerTokens = tokenEstimator.estimate(header);
        
        // Split remaining rows into chunks
        StringBuilder currentChunk = new StringBuilder(header);
        int currentTokens = headerTokens;
        
        for (int i = 2; i < lines.length; i++) {
            String row = lines[i];
            int rowTokens = tokenEstimator.estimate(row);
            
            // Check if adding this row would exceed limit
            if (currentTokens + rowTokens > maxTokensPerChunk && currentChunk.length() > header.length()) {
                // Save current chunk and start new one with header
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder(header);
                currentTokens = headerTokens;
            }
            
            currentChunk.append("\n").append(row);
            currentTokens += rowTokens;
        }
        
        // Save remaining chunk
        if (currentChunk.length() > header.length()) {
            chunks.add(currentChunk.toString().trim());
        }
        
        LOG.debugf("Split large table into %d chunks", chunks.size());
        return chunks;
    }

    /**
     * Builder for regular content chunks.
     */
    private static class ChunkBuilder {
        private final StringBuilder content = new StringBuilder();

        void addLine(String line) {
            content.append(line).append("\n");
        }

        boolean hasContent() {
            return content.length() > 0;
        }

        String content() {
            return content.toString();
        }

        String build() {
            return content.toString().trim();
        }
    }

    /**
     * Builder for table chunks.
     */
    private static class TableBuilder {
        private final StringBuilder content = new StringBuilder();

        void addLine(String line) {
            content.append(line).append("\n");
        }

        boolean hasContent() {
            return content.length() > 0;
        }

        String build() {
            return content.toString().trim();
        }
    }
}
