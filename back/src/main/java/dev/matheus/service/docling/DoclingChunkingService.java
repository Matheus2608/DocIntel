package dev.matheus.service.docling;

import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for chunking markdown documents into semantically meaningful chunks.
 * Implements semantic chunking based on industry best practices:
 * - Respects document structure (headings, paragraphs, tables, lists)
 * - Keeps tables and lists atomic (never split)
 * - Splits at logical boundaries (heading > paragraph > sentence)
 * - Preserves context and semantic completeness
 */
@ApplicationScoped
public class DoclingChunkingService {

    private static final Logger LOG = Logger.getLogger(DoclingChunkingService.class);
    
    // Regex patterns for markdown structure detection
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\s*\\|[-:| ]+\\|\\s*$", Pattern.MULTILINE);
    private static final String LIST_ITEM_REGEX = "^[-*+]\\s+.*";
    private static final String NUMBERED_LIST_REGEX = "^\\d+\\.\\s+.*";
    private static final String HEADING_REGEX = "^#{1,6}\\s+.*";
    private static final String CODE_FENCE = "```";
    
    // Chunking strategy thresholds
    private static final double LARGE_ATOMIC_UNIT_THRESHOLD = 0.8;  // 80% of maxTokens
    private static final double HEADING_FLUSH_THRESHOLD = 0.2;       // 20% of maxTokens
    private static final int MINIMUM_CHUNK_CONTENT_TOKENS = 100;     // Minimum tokens for meaningful chunk
    
    @Inject
    TokenEstimator tokenEstimator;
    
    @Inject
    ContentTypeDetector contentTypeDetector;

    /**
     * Chunk markdown document into semantic chunks.
     * 
     * @param documentFile The parent document file
     * @param markdown The markdown content to chunk
     * @param maxTokens Maximum tokens per chunk
     * @return List of DocumentChunk entities
     */
    public List<DocumentChunk> chunkMarkdown(DocumentFile documentFile, String markdown, int maxTokens) {
        if (markdown == null || markdown.trim().isEmpty()) {
            LOG.debug("Empty markdown content, returning empty chunk list");
            return List.of();
        }
        
        LOG.debugf("Chunking markdown document: %s with maxTokens=%d", documentFile.fileName, maxTokens);
        
        // Parse markdown into semantic units
        List<SemanticUnit> units = parseMarkdown(markdown);
        LOG.debugf("Parsed %d semantic units", units.size());
        
        // Group units into chunks
        List<DocumentChunk> chunks = groupUnitsIntoChunks(units, documentFile, maxTokens);
        LOG.debugf("Created %d chunks", chunks.size());
        
        return chunks;
    }

    /**
     * Parse markdown content into semantic units.
     * Each unit represents a heading, paragraph, table, list, or code block.
     */
    private List<SemanticUnit> parseMarkdown(String markdown) {
        List<SemanticUnit> units = new ArrayList<>();
        String[] lines = markdown.split("\n", -1);
        
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            
            // Skip empty lines
            if (line.trim().isEmpty()) {
                i++;
                continue;
            }
            
            // Check for heading
            if (line.matches(HEADING_REGEX)) {
                units.add(parseHeading(line));
                i++;
                continue;
            }
            
            // Check for table (look ahead for separator)
            if (line.contains("|") && i + 1 < lines.length) {
                int tableEnd = findTableEnd(lines, i);
                if (tableEnd > i) {
                    units.add(parseTable(lines, i, tableEnd));
                    i = tableEnd;
                    continue;
                }
            }
            
            // Check for list
            if (isListItem(line)) {
                int listEnd = findListEnd(lines, i);
                units.add(parseList(lines, i, listEnd));
                i = listEnd;
                continue;
            }
            
            // Check for code block
            if (line.trim().startsWith(CODE_FENCE)) {
                int codeEnd = findCodeEnd(lines, i);
                units.add(parseCode(lines, i, codeEnd));
                i = codeEnd;
                continue;
            }
            
            // Otherwise, it's a paragraph
            int paraEnd = findParagraphEnd(lines, i);
            units.add(parseParagraph(lines, i, paraEnd));
            i = paraEnd;
        }
        
        return units;
    }

    private SemanticUnit parseHeading(String line) {
        Matcher matcher = HEADING_PATTERN.matcher(line);
        if (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2);
            return new SemanticUnit(UnitType.HEADING, line, level, text);
        }
        return new SemanticUnit(UnitType.HEADING, line, 1, line);
    }
    
    /**
     * Check if a line is a list item (bullet or numbered).
     */
    private boolean isListItem(String line) {
        return line.matches(LIST_ITEM_REGEX) || line.matches(NUMBERED_LIST_REGEX);
    }

    private int findTableEnd(String[] lines, int start) {
        // A table must have a separator line (|---|---|)
        boolean foundSeparator = false;
        int i = start;
        
        // Check if next line is separator
        if (i + 1 < lines.length && lines[i + 1].matches("^\\s*\\|[-:| ]+\\|\\s*$")) {
            foundSeparator = true;
            i = i + 2; // Start after separator
        } else {
            return start; // Not a table
        }
        
        // Continue until we find a line that doesn't contain |
        while (i < lines.length && lines[i].contains("|")) {
            i++;
        }
        
        return foundSeparator ? i : start;
    }

    private int findListEnd(String[] lines, int start) {
        int i = start + 1;
        while (i < lines.length) {
            String line = lines[i];
            // Continue if line is list item or empty (for multi-line list items)
            if (line.trim().isEmpty()) {
                // Check if next line continues the list
                if (i + 1 < lines.length && isListItem(lines[i + 1])) {
                    i++;
                    continue;
                }
                break; // Empty line ends list
            }
            if (isListItem(line)) {
                i++;
                continue;
            }
            break;
        }
        return i;
    }

    /**
     * Build content from array of lines.
     */
    private String buildContent(String[] lines, int start, int end, boolean joinWithSpace) {
        StringBuilder content = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                content.append(joinWithSpace ? " " : "\n");
            }
            content.append(joinWithSpace ? lines[i].trim() : lines[i]);
        }
        return content.toString().trim();
    }

    private SemanticUnit parseList(String[] lines, int start, int end) {
        return new SemanticUnit(UnitType.LIST, buildContent(lines, start, end, false), 0, null);
    }

    private int findCodeEnd(String[] lines, int start) {
        int i = start + 1;
        while (i < lines.length && !lines[i].trim().startsWith(CODE_FENCE)) {
            i++;
        }
        return i < lines.length ? i + 1 : lines.length;
    }

    private SemanticUnit parseCode(String[] lines, int start, int end) {
        return new SemanticUnit(UnitType.CODE, buildContent(lines, start, end, false), 0, null);
    }

    private int findParagraphEnd(String[] lines, int start) {
        int i = start + 1;
        while (i < lines.length) {
            String line = lines[i];
            // Paragraph ends at blank line or structural element
            if (line.trim().isEmpty()) {
                break;
            }
            if (isStructuralElement(line)) {
                break;
            }
            i++;
        }
        return i;
    }
    
    /**
     * Check if line is a structural markdown element (heading, list, code, table).
     */
    private boolean isStructuralElement(String line) {
        return line.matches(HEADING_REGEX) || 
               isListItem(line) ||
               line.trim().startsWith(CODE_FENCE) ||
               line.contains("|");
    }

    private SemanticUnit parseParagraph(String[] lines, int start, int end) {
        return new SemanticUnit(UnitType.PARAGRAPH, buildContent(lines, start, end, true), 0, null);
    }

    private SemanticUnit parseTable(String[] lines, int start, int end) {
        return new SemanticUnit(UnitType.TABLE, buildContent(lines, start, end, false), 0, null);
    }

    /**
     * Handle adding a heading unit to chunks.
     */
    private void handleHeading(SemanticUnit unit, int unitIndex, List<SemanticUnit> units, int maxTokens,
                              List<SemanticUnit> currentChunkUnits, List<DocumentChunk> chunks,
                              DocumentFile documentFile, int currentTokens, int unitTokens) {
        // Check if there's content after this heading
        boolean hasContentAfter = unitIndex + 1 < units.size();
        
        // Flush before heading if current chunk has any content
        // This ensures each heading starts a new chunk (strong section boundaries)
        String currentSectionHeading = getCurrentSectionHeading(currentChunkUnits);
        Integer currentHeadingLevel = getCurrentHeadingLevel(currentChunkUnits);
        
        if (!currentChunkUnits.isEmpty() && 
            (currentTokens > maxTokens * HEADING_FLUSH_THRESHOLD || unit.headingLevel <= 2)) {
            chunks.add(createChunk(currentChunkUnits, documentFile, chunks.size(),
                                  currentSectionHeading, currentHeadingLevel));
            currentChunkUnits.clear();
        }
        
        currentChunkUnits.add(unit);
    }
    
    /**
     * Get current section heading from units.
     */
    private String getCurrentSectionHeading(List<SemanticUnit> units) {
        return units.stream()
                .filter(u -> u.type == UnitType.HEADING)
                .map(u -> u.headingText)
                .reduce((first, second) -> second)
                .orElse(null);
    }
    
    /**
     * Get current heading level from units.
     */
    private Integer getCurrentHeadingLevel(List<SemanticUnit> units) {
        return units.stream()
                .filter(u -> u.type == UnitType.HEADING)
                .map(u -> u.headingLevel)
                .reduce((first, second) -> second)
                .orElse(null);
    }
    
    /**
     * Handle adding a paragraph unit, potentially splitting if too large.
     */
    private void handleParagraph(SemanticUnit unit, int unitTokens, int maxTokens,
                                List<SemanticUnit> currentChunkUnits, List<DocumentChunk> chunks,
                                DocumentFile documentFile, String sectionHeading, Integer headingLevel,
                                int currentTokens) {
        // If adding this paragraph exceeds limit
        if (currentTokens + unitTokens > maxTokens) {
            // If we have existing content, flush it first
            if (!currentChunkUnits.isEmpty()) {
                chunks.add(createChunk(currentChunkUnits, documentFile, chunks.size(),
                                      sectionHeading, headingLevel));
                currentChunkUnits.clear();
            }
            
            // If paragraph itself is too large, split at sentence boundaries
            if (unitTokens > maxTokens) {
                splitAndAddParagraph(unit, maxTokens, currentChunkUnits, chunks, 
                                   documentFile, sectionHeading, headingLevel);
                return;
            }
        }
        
        // Add paragraph to current chunk
        currentChunkUnits.add(unit);
    }
    
    /**
     * Split an oversized paragraph at sentence boundaries and add to chunks.
     */
    private void splitAndAddParagraph(SemanticUnit unit, int maxTokens, 
                                     List<SemanticUnit> currentChunkUnits, List<DocumentChunk> chunks,
                                     DocumentFile documentFile, String sectionHeading, Integer headingLevel) {
        List<String> sentences = splitIntoSentences(unit.content);
        List<String> currentSentences = new ArrayList<>();
        int sentenceTokens = 0;
        
        for (String sentence : sentences) {
            int senTokens = tokenEstimator.estimate(sentence);
            if (sentenceTokens + senTokens > maxTokens && !currentSentences.isEmpty()) {
                // Flush current sentences as a chunk
                SemanticUnit sentenceUnit = new SemanticUnit(UnitType.PARAGRAPH, 
                                                             String.join(" ", currentSentences), 0, null);
                chunks.add(createChunk(List.of(sentenceUnit), documentFile, chunks.size(),
                                      sectionHeading, headingLevel));
                currentSentences.clear();
                sentenceTokens = 0;
            }
            currentSentences.add(sentence);
            sentenceTokens += senTokens;
        }
        
        // Add remaining sentences to current chunk
        if (!currentSentences.isEmpty()) {
            currentChunkUnits.add(new SemanticUnit(UnitType.PARAGRAPH, 
                                                   String.join(" ", currentSentences), 0, null));
        }
    }
    
    /**
     * Check if unit type is atomic (should never be split).
     */
    private boolean isAtomicUnit(UnitType type) {
        return type == UnitType.TABLE || type == UnitType.LIST || type == UnitType.CODE;
    }
    
    /**
     * Handle adding an atomic unit to chunks.
     */
    private void handleAtomicUnit(SemanticUnit unit, int unitTokens, int maxTokens,
                                   List<SemanticUnit> currentChunkUnits, List<DocumentChunk> chunks,
                                   DocumentFile documentFile, String sectionHeading, Integer headingLevel) {
        // If adding this atomic unit exceeds limit and we have existing content, flush first
        if (!currentChunkUnits.isEmpty() && 
            tokenEstimator.estimate(buildContentFromUnits(currentChunkUnits)) + unitTokens > maxTokens) {
            chunks.add(createChunk(currentChunkUnits, documentFile, chunks.size(), 
                                  sectionHeading, headingLevel));
            currentChunkUnits.clear();
        }
        // Add atomic unit (even if it exceeds maxTokens)
        currentChunkUnits.add(unit);
    }
    
    /**
     * Check if we should flush after adding an atomic unit.
     */
    private boolean shouldFlushAfterAtomicUnit(int unitTokens, int maxTokens, List<SemanticUnit> currentChunkUnits) {
        return unitTokens > maxTokens * LARGE_ATOMIC_UNIT_THRESHOLD && !currentChunkUnits.isEmpty();
    }
    
    /**
     * Build content string from list of semantic units.
     */
    private String buildContentFromUnits(List<SemanticUnit> units) {
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < units.size(); i++) {
            if (i > 0) {
                contentBuilder.append("\n\n");
            }
            contentBuilder.append(units.get(i).content);
        }
        return contentBuilder.toString();
    }
    
    /**
     * Group semantic units into chunks respecting token limits and boundaries.
     */
    private List<DocumentChunk> groupUnitsIntoChunks(List<SemanticUnit> units, DocumentFile documentFile, int maxTokens) {
        List<DocumentChunk> chunks = new ArrayList<>();
        List<SemanticUnit> currentChunkUnits = new ArrayList<>();
        int currentTokens = 0;
        String currentSectionHeading = null;
        Integer currentHeadingLevel = null;
        
        for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
            SemanticUnit unit = units.get(unitIndex);
            int unitTokens = tokenEstimator.estimate(unit.content);
            
            // Handle atomic units (tables, lists, code) - never split
            if (isAtomicUnit(unit.type)) {
                handleAtomicUnit(unit, unitTokens, maxTokens, currentChunkUnits, chunks, 
                               documentFile, currentSectionHeading, currentHeadingLevel);
                if (shouldFlushAfterAtomicUnit(unitTokens, maxTokens, currentChunkUnits)) {
                    chunks.add(createChunk(currentChunkUnits, documentFile, chunks.size(),
                                          currentSectionHeading, currentHeadingLevel));
                    currentChunkUnits.clear();
                    currentTokens = 0;
                } else {
                    currentTokens += unitTokens;
                }
                continue;
            }
            
            // Handle headings - they define sections
            if (unit.type == UnitType.HEADING) {
                handleHeading(unit, unitIndex, units, maxTokens, currentChunkUnits, chunks,
                            documentFile, currentTokens, unitTokens);
                // Update section context and tokens
                currentSectionHeading = unit.headingText;
                currentHeadingLevel = unit.headingLevel;
                currentTokens = tokenEstimator.estimate(buildContentFromUnits(currentChunkUnits));
                continue;
            }
            
            // Handle paragraphs - split if necessary
            if (unit.type == UnitType.PARAGRAPH) {
                handleParagraph(unit, unitTokens, maxTokens, currentChunkUnits, chunks,
                              documentFile, currentSectionHeading, currentHeadingLevel, currentTokens);
                currentTokens = tokenEstimator.estimate(buildContentFromUnits(currentChunkUnits));
            }
        }
        
        // Flush remaining units
        if (!currentChunkUnits.isEmpty()) {
            // If the only remaining unit is a heading, try to add it to the last chunk
            if (currentChunkUnits.size() == 1 && currentChunkUnits.get(0).type == UnitType.HEADING && !chunks.isEmpty()) {
                // Get last chunk and append this heading to it
                DocumentChunk lastChunk = chunks.get(chunks.size() - 1);
                lastChunk.content = lastChunk.content + "\n\n" + currentChunkUnits.get(0).content;
                lastChunk.tokenCount = tokenEstimator.estimate(lastChunk.content);
                lastChunk.contentType = contentTypeDetector.detect(lastChunk.content);
            } else {
                chunks.add(createChunk(currentChunkUnits, documentFile, chunks.size(),
                                      currentSectionHeading, currentHeadingLevel));
            }
        }
        
        return chunks;
    }

    /**
     * Split text into sentences at proper boundaries.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // Split at sentence boundaries: . ! ? followed by space or end
        String[] parts = text.split("(?<=[.!?])\\s+");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part.trim());
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    /**
     * Create a DocumentChunk from semantic units.
     */
    private DocumentChunk createChunk(List<SemanticUnit> units, DocumentFile documentFile, 
                                     int position, String sectionHeading, Integer headingLevel) {
        DocumentChunk chunk = new DocumentChunk();
        String content = buildContentFromUnits(units);
        
        // Set basic fields
        chunk.documentFile = documentFile;
        chunk.content = content;
        chunk.position = position;
        chunk.tokenCount = tokenEstimator.estimate(content);
        chunk.contentType = contentTypeDetector.detect(content);
        
        // Set section heading metadata
        setChunkHeadingMetadata(chunk, units, sectionHeading, headingLevel);
        
        return chunk;
    }
    
    /**
     * Set chunk heading metadata from units or context.
     */
    private void setChunkHeadingMetadata(DocumentChunk chunk, List<SemanticUnit> units, 
                                        String contextHeading, Integer contextLevel) {
        SemanticUnit firstHeading = units.stream()
                .filter(u -> u.type == UnitType.HEADING)
                .findFirst()
                .orElse(null);
        
        if (firstHeading != null) {
            chunk.sectionHeading = firstHeading.headingText;
            chunk.headingLevel = firstHeading.headingLevel;
        } else if (contextHeading != null) {
            chunk.sectionHeading = contextHeading;
            chunk.headingLevel = contextLevel;
        }
    }

    /**
     * Semantic unit types.
     */
    private enum UnitType {
        HEADING, PARAGRAPH, TABLE, LIST, CODE
    }

    /**
     * Represents a semantic unit in the markdown document.
     */
    private static class SemanticUnit {
        final UnitType type;
        final String content;
        final int headingLevel;
        final String headingText;
        
        SemanticUnit(UnitType type, String content, int headingLevel, String headingText) {
            this.type = type;
            this.content = content;
            this.headingLevel = headingLevel;
            this.headingText = headingText;
        }
    }
}
