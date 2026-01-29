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
                    // Table ended - save it as a chunk
                    chunks.add(currentTable.build());
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
            chunks.add(currentTable.build());
        }
        
        // Save any remaining content
        if (currentChunk.hasContent()) {
            chunks.add(currentChunk.build());
        }
        
        LOG.debugf("Split markdown into %d chunks", chunks.size());
        return chunks;
    }

    private boolean isTableLine(String line) {
        return line.trim().contains("|");
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
