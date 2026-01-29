package dev.matheus.service.docling;

import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DoclingChunkingService - User Story 3: Generate Semantically Meaningful Chunks
 * 
 * TDD RED Phase: These tests MUST FAIL because DoclingChunkingService does not exist yet.
 * 
 * Tests verify a SOLID semantic chunking strategy that:
 * - Respects document structure (sections, paragraphs, tables, lists)
 * - Produces independently meaningful chunks for RAG
 * - Never splits semantic units across boundaries unless unavoidable
 * - Handles size limits gracefully with logical boundary splits
 * - Preserves context (tables with explanatory text)
 * 
 * Tests cover:
 * - T039: Semantic boundary detection
 * - T040: Table atomicity preservation
 * - T041: Paragraph boundary respect
 * - T042: Logical boundary splits when exceeding limits
 */
@QuarkusTest
class DoclingChunkingServiceTest {

    @Inject
    DoclingChunkingService doclingChunkingService;

    @Inject
    TokenEstimator tokenEstimator;

    /**
     * T039: Test semantic boundary detection
     * 
     * User Story 3, Requirement FR-006:
     * Given a document with headings, paragraphs, tables, and lists,
     * When the system chunks the content,
     * Then chunks MUST split at semantic boundaries (not mid-content)
     * AND headings MUST stay with their content
     * AND tables, lists, and paragraphs MUST remain intact
     */
    @Test
    void shouldDetectSemanticBoundaries() {
        // Arrange - Create realistic markdown document with clear structure
        String markdown = createMarkdownWithStructure();
        DocumentFile documentFile = createTestDocumentFile("test-structure.md");

        // Act
        List<DocumentChunk> chunks = doclingChunkingService.chunkMarkdown(documentFile, markdown, 1000);

        // Assert - Verify chunks respect semantic boundaries
        assertThat(chunks)
                .as("Document should be split into multiple semantic chunks")
                .hasSizeGreaterThan(1);

        // Verify no paragraph is split mid-sentence
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            
            // If chunk ends without proper sentence terminator, it might be an incomplete paragraph
            // (unless it's a heading, list item, or table)
            if (!content.endsWith(".") && !content.endsWith("!") && !content.endsWith("?") 
                && !content.endsWith("|") && !content.matches(".*[#-]\\s*$")) {
                
                // Check if this is end of list or heading (acceptable)
                boolean isListOrHeading = content.lines()
                        .reduce((first, second) -> second)
                        .map(line -> line.matches("^[#-].*") || line.matches("^\\d+\\..*"))
                        .orElse(false);
                
                assertThat(isListOrHeading || content.endsWith("|"))
                        .as("Chunk should not end mid-sentence unless it's a list, heading, or table: " + content.substring(Math.max(0, content.length() - 50)))
                        .isTrue();
            }
        }

        // Verify tables are not split (no chunk should start or end mid-table)
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            long tableLineCount = content.lines().filter(line -> line.contains("|")).count();
            
            if (tableLineCount > 0) {
                // If chunk contains table lines, verify it's a complete table
                // A complete table has header, separator, and data rows
                boolean hasTableSeparator = content.lines()
                        .anyMatch(line -> line.matches("^\\s*\\|[-:| ]+\\|\\s*$"));
                
                assertThat(hasTableSeparator)
                        .as("Chunk with table lines should contain complete table with separator: " + chunk.content)
                        .isTrue();
            }
        }

        // Verify headings stay with their content
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            String content = chunk.content.trim();
            
            // If chunk starts with heading, it should have content following it
            if (content.startsWith("#")) {
                String[] lines = content.split("\n");
                assertThat(lines.length)
                        .as("Heading chunk should have content after the heading")
                        .isGreaterThan(1);
            }
            
            // If chunk has sectionHeading metadata, verify it's meaningful
            if (chunk.sectionHeading != null) {
                assertThat(chunk.sectionHeading)
                        .as("Section heading metadata should be non-empty")
                        .isNotBlank();
                
                assertThat(chunk.headingLevel)
                        .as("Heading level should be between 1 and 6")
                        .isBetween(1, 6);
            }
        }

        // Verify lists are not split mid-list
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            String[] lines = content.split("\n");
            
            // Check if chunk ends with a list item
            if (lines.length > 0) {
                String lastLine = lines[lines.length - 1].trim();
                boolean endsWithListItem = lastLine.matches("^[-*+]\\s+.*") || lastLine.matches("^\\d+\\.\\s+.*");
                
                if (endsWithListItem && chunk.position < chunks.size() - 1) {
                    // Check if next chunk starts with a list item
                    DocumentChunk nextChunk = chunks.stream()
                            .filter(c -> c.position == chunk.position + 1)
                            .findFirst()
                            .orElse(null);
                    
                    if (nextChunk != null) {
                        String nextFirstLine = nextChunk.content.trim().split("\n")[0].trim();
                        boolean nextStartsWithList = nextFirstLine.matches("^[-*+]\\s+.*") || nextFirstLine.matches("^\\d+\\.\\s+.*");
                        
                        // If both end and start with list items, they should be same list (not split)
                        // This is acceptable only if there's a heading or blank line between
                        if (nextStartsWithList) {
                            boolean hasHeadingOrBreak = content.endsWith("\n\n") || nextChunk.content.startsWith("#");
                            assertThat(hasHeadingOrBreak)
                                    .as("List should not be split across chunks without clear boundary")
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    /**
     * T040: Test table atomicity preservation
     * 
     * User Story 3, Requirement FR-007 and Acceptance Scenario 1:
     * Given a document with a large table (>500 tokens),
     * When the system chunks the content,
     * Then the table MUST be kept as a single atomic chunk
     * AND table structure MUST remain intact (not split across boundaries)
     * AND chunk MUST be marked with appropriate content type
     */
    @Test
    void shouldNotSplitTablesAcrossChunks() {
        // Arrange - Create document with large table
        String markdown = createMarkdownWithLargeTable();
        DocumentFile documentFile = createTestDocumentFile("test-large-table.md");

        // Act
        List<DocumentChunk> chunks = doclingChunkingService.chunkMarkdown(documentFile, markdown, 1000);

        // Assert - Find chunks containing table content
        List<DocumentChunk> tableChunks = chunks.stream()
                .filter(chunk -> chunk.content.contains("|"))
                .toList();

        assertThat(tableChunks)
                .as("Document should have at least one chunk with table content")
                .isNotEmpty();

        // Verify each table is complete and not split
        for (DocumentChunk tableChunk : tableChunks) {
            String content = tableChunk.content;
            
            // Verify table has all required components
            boolean hasHeaderRow = content.lines()
                    .anyMatch(line -> line.trim().startsWith("|") && !line.matches(".*[-:]+.*"));
            
            boolean hasSeparator = content.lines()
                    .anyMatch(line -> line.matches("^\\s*\\|[-:| ]+\\|\\s*$"));
            
            long dataRowCount = content.lines()
                    .filter(line -> line.trim().startsWith("|"))
                    .filter(line -> !line.matches(".*[-:]+.*"))
                    .count();

            assertThat(hasHeaderRow)
                    .as("Table chunk must contain header row")
                    .isTrue();
            
            assertThat(hasSeparator)
                    .as("Table chunk must contain separator row")
                    .isTrue();
            
            assertThat(dataRowCount)
                    .as("Table chunk must contain data rows")
                    .isGreaterThanOrEqualTo(2); // At least header + 1 data row

            // Verify content type is appropriately marked
            assertThat(tableChunk.contentType)
                    .as("Chunk with table should be marked as TABLE or MIXED")
                    .isIn(ContentType.TABLE, ContentType.MIXED);

            // Verify table is large (>500 tokens as per requirement)
            int estimatedTokens = tokenEstimator.estimate(content);
            assertThat(estimatedTokens)
                    .as("Test table should be large (>500 tokens) to validate atomicity")
                    .isGreaterThan(500);
        }

        // Verify no table is split across multiple chunks
        // If we find a table separator in one chunk, all table lines should be in same chunk
        for (DocumentChunk chunk : chunks) {
            boolean hasSeparator = chunk.content.lines()
                    .anyMatch(line -> line.matches("^\\s*\\|[-:| ]+\\|\\s*$"));
            
            if (hasSeparator) {
                // This chunk has a table separator, so it should contain the complete table
                long tableLines = chunk.content.lines()
                        .filter(line -> line.trim().contains("|"))
                        .count();
                
                assertThat(tableLines)
                        .as("Chunk with table separator should have complete table (header + separator + data)")
                        .isGreaterThanOrEqualTo(3);
                
                // Check that previous and next chunks don't have orphaned table lines
                if (chunk.position > 0) {
                    DocumentChunk prevChunk = chunks.stream()
                            .filter(c -> c.position == chunk.position - 1)
                            .findFirst()
                            .orElse(null);
                    
                    if (prevChunk != null) {
                        String lastLine = prevChunk.content.trim().split("\n")[prevChunk.content.trim().split("\n").length - 1];
                        assertThat(lastLine.contains("|"))
                                .as("Previous chunk should not end with table line (table should be atomic)")
                                .isFalse();
                    }
                }
            }
        }
    }

    /**
     * T041: Test paragraph boundary respect
     * 
     * User Story 3, Requirement FR-007:
     * Given a document with multiple paragraphs,
     * When the system chunks the content,
     * Then paragraphs MUST NOT be split mid-sentence
     * AND chunks MUST end at paragraph breaks (double newline)
     * AND each chunk MUST be semantically complete
     */
    @Test
    void shouldRespectParagraphBoundaries() {
        // Arrange - Create document with clear paragraph structure
        String markdown = createMarkdownWithParagraphs();
        DocumentFile documentFile = createTestDocumentFile("test-paragraphs.md");

        // Act
        List<DocumentChunk> chunks = doclingChunkingService.chunkMarkdown(documentFile, markdown, 800);

        // Assert
        assertThat(chunks)
                .as("Document with multiple paragraphs should create multiple chunks")
                .hasSizeGreaterThan(1);

        // Verify no paragraph is split mid-sentence
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            
            // Skip chunks that are headings or lists only
            boolean isHeadingOnly = content.matches("^#+\\s+.*$");
            boolean isListOnly = content.lines().allMatch(line -> 
                    line.matches("^[-*+]\\s+.*") || line.matches("^\\d+\\.\\s+.*") || line.isBlank());
            
            if (!isHeadingOnly && !isListOnly) {
                // Content should end at sentence boundary
                String lastLine = content.lines()
                        .reduce((first, second) -> second)
                        .orElse("");
                
                // Check if last line ends properly (sentence, list item, or table)
                boolean endsAtSentence = lastLine.matches(".*[.!?]\\s*$");
                boolean endsAtList = lastLine.matches("^[-*+]\\s+.*") || lastLine.matches("^\\d+\\.\\s+.*");
                boolean endsAtTable = lastLine.trim().endsWith("|");
                boolean endsAtHeading = lastLine.matches("^#+\\s+.*");
                
                assertThat(endsAtSentence || endsAtList || endsAtTable || endsAtHeading)
                        .as("Chunk should end at proper boundary (sentence/list/table/heading), not mid-sentence. Last line: " + lastLine)
                        .isTrue();
            }
        }

        // Verify chunks split at paragraph boundaries (double newline)
        // Reconstruct original by checking if content has proper paragraph separation
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content;
            
            // If chunk has multiple paragraphs, they should be separated by blank lines
            String[] lines = content.split("\n", -1);
            boolean hasBlankLineSeparation = false;
            
            for (int i = 0; i < lines.length - 1; i++) {
                if (lines[i].trim().isEmpty() && !lines[i + 1].trim().isEmpty()) {
                    hasBlankLineSeparation = true;
                    break;
                }
            }
            
            // If chunk contains multiple paragraphs (detected by blank line), verify proper structure
            if (hasBlankLineSeparation) {
                // Count paragraphs (sequences of non-blank lines)
                int paragraphCount = 0;
                boolean inParagraph = false;
                
                for (String line : lines) {
                    if (!line.trim().isEmpty() && !inParagraph) {
                        paragraphCount++;
                        inParagraph = true;
                    } else if (line.trim().isEmpty()) {
                        inParagraph = false;
                    }
                }
                
                assertThat(paragraphCount)
                        .as("Chunk with blank line separation should have multiple paragraphs")
                        .isGreaterThan(1);
            }
        }

        // Verify semantic completeness - each chunk should make sense independently
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            
            // Check for incomplete sentences (sentences that don't end properly)
            Pattern incompleteSentence = Pattern.compile("[a-z]\\s*$", Pattern.MULTILINE);
            boolean hasIncompleteSentence = incompleteSentence.matcher(content).find();
            
            if (hasIncompleteSentence) {
                // Allow if it's a list item or heading at the end
                String lastLine = content.lines()
                        .reduce((first, second) -> second)
                        .orElse("");
                
                boolean isAcceptableEnding = lastLine.matches("^[-*+#].*") || 
                                             lastLine.matches("^\\d+\\..*") ||
                                             lastLine.trim().endsWith("|");
                
                assertThat(isAcceptableEnding)
                        .as("Chunk should not have incomplete sentences unless ending with list/heading/table")
                        .isTrue();
            }
        }
    }

    /**
     * T042: Test logical boundary splits when exceeding token limit
     * 
     * User Story 3, Requirement FR-008 and Acceptance Scenario 2:
     * Given a document where natural chunks exceed max token limit (2000),
     * When the system chunks the content,
     * Then splits MUST occur at logical boundaries (heading > paragraph > sentence)
     * AND resulting chunks MUST be semantically complete
     * AND splits MUST NOT occur mid-paragraph unless absolutely necessary
     * AND both resulting chunks MUST be independently meaningful
     */
    @Test
    void shouldSplitAtLogicalBoundariesWhenExceedingLimit() {
        // Arrange - Create document with large section that exceeds limit
        String markdown = createMarkdownExceedingTokenLimit();
        DocumentFile documentFile = createTestDocumentFile("test-large-section.md");
        int maxTokens = 1000; // Set limit lower than content to force splits

        // Act
        List<DocumentChunk> chunks = doclingChunkingService.chunkMarkdown(documentFile, markdown, maxTokens);

        // Assert
        assertThat(chunks)
                .as("Large document should be split into multiple chunks")
                .hasSizeGreaterThan(2);

        // Verify no chunk exceeds max token limit (with small tolerance for boundary cases)
        for (DocumentChunk chunk : chunks) {
            int tokenCount = tokenEstimator.estimate(chunk.content);
            
            // Allow some tolerance for tables (which should not be split even if large)
            boolean isTableChunk = chunk.contentType == ContentType.TABLE || 
                                   chunk.content.lines().filter(line -> line.contains("|")).count() > 3;
            
            if (!isTableChunk) {
                assertThat(tokenCount)
                        .as("Regular chunk should not significantly exceed max token limit")
                        .isLessThan((int) (maxTokens * 1.2)); // 20% tolerance for boundary cases
            }
        }

        // Verify splits occur at logical boundaries
        for (int i = 0; i < chunks.size() - 1; i++) {
            DocumentChunk currentChunk = chunks.get(i);
            DocumentChunk nextChunk = chunks.get(i + 1);
            
            String currentContent = currentChunk.content.trim();
            String nextContent = nextChunk.content.trim();
            
            // Check if split occurred at heading boundary (preferred)
            boolean splitAtHeading = nextContent.startsWith("#");
            
            // Check if split occurred at paragraph boundary (blank line)
            boolean splitAtParagraph = currentContent.endsWith("\n") || 
                                       currentContent.matches(".*[.!?]\\s*$");
            
            // Check if split occurred at section boundary
            boolean splitAtSection = splitAtHeading || 
                                     currentContent.lines().reduce((first, second) -> second)
                                             .map(line -> line.matches(".*[.!?]$"))
                                             .orElse(false);
            
            // At least one logical boundary condition should be met
            assertThat(splitAtHeading || splitAtParagraph || splitAtSection)
                    .as("Split should occur at logical boundary (heading, paragraph, or section). " +
                        "Current ends with: '" + currentContent.substring(Math.max(0, currentContent.length() - 50)) + "', " +
                        "Next starts with: '" + nextContent.substring(0, Math.min(50, nextContent.length())) + "'")
                    .isTrue();
        }

        // Verify semantic completeness - each chunk should be independently meaningful
        for (DocumentChunk chunk : chunks) {
            String content = chunk.content.trim();
            
            // Chunk should not start mid-sentence (unless it's continuation after table/heading)
            String firstLine = content.split("\n")[0].trim();
            boolean startsWithCapital = firstLine.matches("^[A-Z#\\-*+\\d|].*");
            
            assertThat(startsWithCapital)
                    .as("Chunk should start with capital letter, heading, list, or table (not mid-sentence)")
                    .isTrue();
            
            // Chunk should not end mid-sentence (verified earlier, but double-check)
            String lastLine = content.lines().reduce((first, second) -> second).orElse("");
            boolean endsAtBoundary = lastLine.matches(".*[.!?]\\s*$") || 
                                     lastLine.matches("^[-*+#].*") || 
                                     lastLine.matches("^\\d+\\..*") ||
                                     lastLine.trim().endsWith("|");
            
            assertThat(endsAtBoundary)
                    .as("Chunk should end at proper boundary")
                    .isTrue();
        }

        // Verify both parts of split sections are meaningful (have context)
        for (int i = 0; i < chunks.size() - 1; i++) {
            DocumentChunk currentChunk = chunks.get(i);
            DocumentChunk nextChunk = chunks.get(i + 1);
            
            // If chunks share same section heading, both should have substantial content
            if (currentChunk.sectionHeading != null && 
                currentChunk.sectionHeading.equals(nextChunk.sectionHeading)) {
                
                int currentTokens = tokenEstimator.estimate(currentChunk.content);
                int nextTokens = tokenEstimator.estimate(nextChunk.content);
                
                assertThat(currentTokens)
                        .as("First part of split section should have substantial content")
                        .isGreaterThan(100); // At least 100 tokens
                
                assertThat(nextTokens)
                        .as("Second part of split section should have substantial content")
                        .isGreaterThan(100); // At least 100 tokens
            }
        }

        // Verify metadata is properly maintained
        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.position)
                    .as("Chunk position should be set")
                    .isNotNull();
            
            assertThat(chunk.tokenCount)
                    .as("Token count should be calculated")
                    .isGreaterThan(0);
            
            assertThat(chunk.contentType)
                    .as("Content type should be identified")
                    .isNotNull();
        }
    }

    // ========== Helper Methods ==========

    private DocumentFile createTestDocumentFile(String filename) {
        DocumentFile documentFile = new DocumentFile();
        documentFile.id = "test-doc-" + System.currentTimeMillis();
        documentFile.fileName = filename;
        documentFile.fileType = "text/markdown";
        documentFile.fileSize = 1024L;
        return documentFile;
    }

    /**
     * Create markdown document with clear structure:
     * - Multiple sections with headings
     * - Paragraphs with proper spacing
     * - Table with data
     * - Bullet and numbered lists
     */
    private String createMarkdownWithStructure() {
        return """
                # Product Documentation
                
                This document provides comprehensive information about our enterprise software product.
                The product is designed to help organizations manage their data efficiently.
                
                ## Features Overview
                
                Our product offers several key capabilities that distinguish it from competitors.
                These features have been carefully designed based on customer feedback and industry best practices.
                
                ### Data Management
                
                The system provides robust data management capabilities including:
                
                - Real-time data synchronization across multiple nodes
                - Automated backup and recovery mechanisms
                - Advanced data validation and quality checks
                - Comprehensive audit logging for compliance
                
                ### Performance Metrics
                
                The following table shows our system's performance characteristics:
                
                | Metric | Value | Unit | Notes |
                |--------|-------|------|-------|
                | Throughput | 10000 | req/sec | Peak capacity under load testing |
                | Latency | 50 | ms | P99 response time for read operations |
                | Availability | 99.99 | % | Annual uptime SLA commitment |
                | Data Durability | 99.999999999 | % | Eleven nines durability guarantee |
                
                ## Implementation Guide
                
                To implement our product successfully, organizations should follow these steps:
                
                1. Conduct thorough requirements analysis with stakeholders
                2. Design the system architecture based on scalability needs
                3. Configure the database schema according to data models
                4. Set up monitoring and alerting for production readiness
                5. Train end-users on system functionality and best practices
                
                ### Configuration Parameters
                
                The system requires several configuration parameters to be set during deployment.
                These parameters control system behavior and resource allocation.
                Each parameter has a default value but can be customized based on organizational needs.
                
                ## Conclusion
                
                This documentation provides the foundation for understanding and implementing our product.
                For additional support, please contact our technical team.
                """;
    }

    /**
     * Create markdown with a large table (>500 tokens) that should remain atomic
     */
    private String createMarkdownWithLargeTable() {
        return """
                # Financial Report Q4 2024
                
                This section presents detailed financial data for the fourth quarter.
                
                ## Revenue Breakdown by Region and Product Line
                
                | Region | Product Category | Q1 Revenue | Q2 Revenue | Q3 Revenue | Q4 Revenue | Total Annual | Growth Rate | Market Share | Customer Count | Avg Deal Size |
                |--------|------------------|------------|------------|------------|------------|--------------|-------------|--------------|----------------|---------------|
                | North America | Enterprise Software | $5,234,000 | $5,890,000 | $6,123,000 | $7,456,000 | $24,703,000 | 42.5% | 18.3% | 142 | $173,960 |
                | North America | Cloud Services | $3,456,000 | $4,012,000 | $4,567,000 | $5,234,000 | $17,269,000 | 51.6% | 22.1% | 234 | $73,798 |
                | North America | Professional Services | $1,234,000 | $1,456,000 | $1,678,000 | $1,890,000 | $6,258,000 | 53.2% | 15.7% | 89 | $70,314 |
                | Europe | Enterprise Software | $4,123,000 | $4,567,000 | $4,890,000 | $5,678,000 | $19,258,000 | 37.7% | 16.2% | 118 | $163,203 |
                | Europe | Cloud Services | $2,890,000 | $3,234,000 | $3,678,000 | $4,123,000 | $13,925,000 | 42.7% | 19.4% | 198 | $70,328 |
                | Europe | Professional Services | $987,000 | $1,123,000 | $1,345,000 | $1,567,000 | $5,022,000 | 58.8% | 13.5% | 67 | $74,955 |
                | Asia Pacific | Enterprise Software | $3,678,000 | $4,234,000 | $4,789,000 | $5,890,000 | $18,591,000 | 60.2% | 14.8% | 156 | $119,174 |
                | Asia Pacific | Cloud Services | $2,345,000 | $2,890,000 | $3,456,000 | $4,123,000 | $12,814,000 | 75.8% | 17.9% | 267 | $48,000 |
                | Asia Pacific | Professional Services | $756,000 | $890,000 | $1,012,000 | $1,234,000 | $3,892,000 | 63.2% | 10.4% | 78 | $49,897 |
                | Latin America | Enterprise Software | $1,456,000 | $1,678,000 | $1,890,000 | $2,345,000 | $7,369,000 | 61.0% | 5.9% | 67 | $109,985 |
                | Latin America | Cloud Services | $890,000 | $1,012,000 | $1,234,000 | $1,567,000 | $4,703,000 | 76.2% | 6.6% | 134 | $35,097 |
                | Latin America | Professional Services | $345,000 | $456,000 | $567,000 | $678,000 | $2,046,000 | 96.5% | 5.5% | 45 | $45,467 |
                | Middle East Africa | Enterprise Software | $1,123,000 | $1,345,000 | $1,567,000 | $1,890,000 | $5,925,000 | 68.3% | 4.7% | 52 | $113,942 |
                | Middle East Africa | Cloud Services | $678,000 | $890,000 | $1,123,000 | $1,456,000 | $4,147,000 | 114.7% | 5.8% | 98 | $42,316 |
                | Middle East Africa | Professional Services | $234,000 | $345,000 | $456,000 | $567,000 | $1,602,000 | 142.3% | 4.3% | 34 | $47,118 |
                
                The table above demonstrates strong growth across all regions and product categories.
                """;
    }

    /**
     * Create markdown with multiple well-defined paragraphs
     */
    private String createMarkdownWithParagraphs() {
        return """
                # Understanding Cloud Computing Architecture
                
                Cloud computing has fundamentally transformed how organizations deploy and manage their IT infrastructure. This paradigm shift represents one of the most significant technological advances in modern enterprise computing. The ability to provision resources on-demand has enabled businesses to scale operations efficiently while reducing capital expenditure on physical hardware.
                
                The core principles of cloud computing rest on several foundational concepts that distinguish it from traditional data center operations. First, resource pooling allows multiple customers to share physical infrastructure while maintaining logical isolation. Second, rapid elasticity enables systems to scale computing resources up or down based on demand automatically. Third, measured service provides transparency into resource utilization and enables pay-per-use pricing models.
                
                ## Infrastructure as a Service (IaaS)
                
                Infrastructure as a Service represents the most fundamental cloud computing model, providing virtualized computing resources over the internet. Organizations using IaaS can rent virtual machines, storage, and networking components without investing in physical hardware. This model offers maximum flexibility as customers control the operating system, applications, and middleware while the cloud provider manages the underlying physical infrastructure.
                
                Major IaaS providers include Amazon Web Services, Microsoft Azure, and Google Cloud Platform. These platforms offer a comprehensive suite of services including compute instances, block storage, object storage, virtual private networks, and load balancers. Customers benefit from geographic distribution of data centers, allowing them to deploy applications close to end-users for optimal performance.
                
                ## Platform as a Service (PaaS)
                
                Platform as a Service builds upon IaaS by providing a complete development and deployment environment in the cloud. PaaS solutions abstract away infrastructure management, allowing developers to focus purely on application logic. The platform handles operating system updates, scaling, and availability automatically without requiring developer intervention.
                
                Common PaaS offerings include Heroku, Google App Engine, and Azure App Service. These platforms support multiple programming languages and frameworks, providing built-in services for databases, caching, message queues, and authentication. Development teams can deploy applications with simple commands, and the platform manages containerization, load balancing, and auto-scaling automatically.
                
                ## Software as a Service (SaaS)
                
                Software as a Service delivers complete applications over the internet on a subscription basis. End-users access these applications through web browsers without installing or maintaining any software locally. The SaaS provider manages all aspects of the application including infrastructure, platform, and application code.
                
                Popular SaaS applications include Salesforce for customer relationship management, Microsoft 365 for productivity software, and Slack for team collaboration. Organizations benefit from predictable subscription costs, automatic updates, and accessibility from any device with internet connectivity. The SaaS model has become dominant for business applications due to its simplicity and lower total cost of ownership.
                
                ## Security Considerations
                
                Cloud security requires a shared responsibility model between the provider and customer. Cloud providers secure the underlying infrastructure including physical data centers, networking equipment, and hypervisor layers. Customers remain responsible for securing their data, managing access controls, and properly configuring cloud services.
                
                Best practices for cloud security include implementing strong identity and access management, encrypting data at rest and in transit, regularly auditing access logs, and maintaining security patches for any customer-managed components. Organizations should also implement network segmentation using virtual private clouds and security groups to limit exposure of sensitive systems.
                """;
    }

    /**
     * Create markdown document that exceeds token limits to test logical boundary splitting
     */
    private String createMarkdownExceedingTokenLimit() {
        return """
                # Comprehensive Guide to Microservices Architecture
                
                Microservices architecture has emerged as a leading approach for building scalable, maintainable enterprise applications. This architectural style structures an application as a collection of loosely coupled services, each implementing a specific business capability. The microservices approach contrasts sharply with traditional monolithic architecture, where all functionality is bundled into a single deployable unit.
                
                ## Core Principles and Design Patterns
                
                The foundation of microservices architecture rests on several critical design principles that guide implementation decisions. Understanding these principles is essential for architects and developers working with distributed systems. Each principle addresses specific challenges inherent in building distributed applications at scale.
                
                ### Service Boundaries and Domain-Driven Design
                
                Defining appropriate service boundaries represents one of the most challenging aspects of microservices architecture. Services should align with business domains identified through domain-driven design techniques. Each service should own its data and business logic, exposing well-defined APIs for interaction with other services. Bounded contexts from domain-driven design provide natural boundaries for microservices, ensuring that services remain cohesive and loosely coupled.
                
                The process of identifying service boundaries begins with understanding the business domain thoroughly. Domain experts and technical teams must collaborate to identify distinct subdomains and their relationships. Strategic design patterns like context mapping help visualize dependencies between different domains. Tactical patterns such as aggregates, entities, and value objects provide building blocks for implementing domain logic within each service.
                
                ### Decentralized Data Management
                
                Traditional monolithic applications typically use a single shared database, but microservices embrace decentralized data management. Each service maintains its own database, choosing the data storage technology best suited to its specific needs. This approach, known as polyglot persistence, allows services to use relational databases, document stores, key-value stores, or graph databases based on access patterns and data structure requirements.
                
                Data consistency across services becomes more complex without a shared database. Distributed transactions using two-phase commit protocols often prove impractical due to performance and availability concerns. Instead, microservices architectures favor eventual consistency through event-driven patterns. Services publish events when their state changes, and other services subscribe to relevant events to update their own data stores accordingly.
                
                ### API Gateway Pattern
                
                Client applications often need to interact with multiple microservices to fulfill a single business operation. Direct client-to-microservice communication creates tight coupling and forces clients to understand the microservices topology. The API Gateway pattern addresses this challenge by providing a single entry point for all client requests. The gateway handles request routing, composition, and protocol translation.
                
                An effective API gateway implementation provides several capabilities beyond simple request routing. It should handle authentication and authorization, enforcing security policies before requests reach backend services. Rate limiting and throttling protect services from excessive load. The gateway can aggregate responses from multiple services, reducing the number of round trips required by client applications. Caching frequently accessed data at the gateway layer improves response times and reduces backend load.
                
                ## Communication Patterns and Inter-Service Communication
                
                Microservices must communicate with each other to implement business processes that span multiple services. The choice of communication patterns significantly impacts system characteristics including performance, reliability, and complexity. Architects must carefully consider synchronous versus asynchronous communication and select appropriate protocols and message formats.
                
                ### Synchronous Communication with REST and gRPC
                
                Synchronous communication creates request-response interactions where the calling service waits for the called service to respond. REST APIs using HTTP have become the de facto standard for synchronous communication in microservices architectures. RESTful services expose resources through well-defined URLs and use standard HTTP methods for operations. JSON has emerged as the preferred data format due to its simplicity and broad language support.
                
                gRPC presents an alternative to REST for synchronous communication, offering superior performance through HTTP/2 and protocol buffers. Protocol buffers provide efficient binary serialization that consumes less bandwidth than JSON. gRPC supports bidirectional streaming, allowing both clients and servers to send multiple messages on a single connection. The framework generates client and server code from service definitions, ensuring type safety and reducing boilerplate code.
                
                ### Asynchronous Messaging Patterns
                
                Asynchronous messaging decouples services by allowing them to communicate without requiring both parties to be available simultaneously. Message brokers like RabbitMQ, Apache Kafka, and AWS SQS facilitate asynchronous communication. Services publish messages to topics or queues, and interested services subscribe to receive relevant messages. This publish-subscribe pattern enables loose coupling and improved scalability.
                
                Event-driven architecture builds upon asynchronous messaging to enable reactive systems. Services publish events representing state changes or significant occurrences. Other services subscribe to events and react by updating their own state or triggering additional business logic. Event sourcing takes this approach further by storing all state changes as a sequence of events, providing a complete audit trail and enabling powerful debugging and analysis capabilities.
                
                ## Deployment and Operational Considerations
                
                Successfully deploying and operating microservices requires robust infrastructure and operational practices. The distributed nature of microservices introduces complexity in deployment, monitoring, and troubleshooting. Modern container orchestration platforms and observability tools help manage this complexity.
                
                ### Container Orchestration with Kubernetes
                
                Kubernetes has become the dominant platform for deploying and managing containerized microservices. It provides automated deployment, scaling, and management of containerized applications across clusters of hosts. Kubernetes abstracts away infrastructure differences, allowing applications to run consistently across on-premises data centers and public cloud environments.
                
                Key Kubernetes concepts include Pods (the smallest deployable units containing one or more containers), Services (stable network endpoints for accessing Pods), and Deployments (declarative specifications for managing Pod replicas). Kubernetes handles service discovery, load balancing, and automated rollouts and rollbacks. ConfigMaps and Secrets manage application configuration and sensitive data separately from container images.
                
                ### Observability and Monitoring
                
                Observability in microservices architectures requires collecting and analyzing metrics, logs, and traces from all services. Metrics provide quantitative measurements of system behavior including request rates, error rates, and latency. Time-series databases like Prometheus efficiently store and query metrics data. Visualization tools like Grafana create dashboards for monitoring system health and performance.
                
                Distributed tracing tracks requests as they flow through multiple services, providing visibility into end-to-end transaction performance. Tools like Jaeger and Zipkin collect trace data and visualize service dependencies and performance bottlenecks. Centralized logging aggregates logs from all services into a searchable repository, enabling troubleshooting and analysis. The ELK stack (Elasticsearch, Logstash, Kibana) and cloud-native solutions like AWS CloudWatch Logs provide comprehensive logging capabilities.
                """;
    }
}
