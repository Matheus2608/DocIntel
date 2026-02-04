package dev.matheus.service.parser;

import dev.matheus.entity.ContentType;
import dev.matheus.entity.DocumentChunk;
import dev.matheus.entity.DocumentFile;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Document parser using Amazon Textract.
 * Extracts text, tables, and forms from documents.
 * 
 * NOTE: This is a template implementation. Requires:
 * - AWS SDK dependencies in pom.xml
 * - AWS credentials configured (IAM role or ~/.aws/credentials)
 * - Textract API enabled in your AWS account
 */
@ApplicationScoped
public class TextractDocumentParser implements DocumentParser {

    private static final Logger LOG = Logger.getLogger(TextractDocumentParser.class);
    
    @ConfigProperty(name = "aws.region", defaultValue = "us-east-1")
    String awsRegion;
    
    @ConfigProperty(name = "textract.enabled", defaultValue = "false")
    boolean textractEnabled;
    
    private TextractClient textractClient;
    
    /**
     * Initialize Textract client lazily
     */
    private TextractClient getClient() {
        if (textractClient == null) {
            textractClient = TextractClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        }
        return textractClient;
    }

    @Override
    public List<DocumentChunk> parse(DocumentFile documentFile, byte[] documentContent) {
        if (!textractEnabled) {
            LOG.warn("Textract is disabled. Enable with textract.enabled=true");
            return List.of();
        }
        
        LOG.infof("Parsing document with Textract: %s", documentFile.fileName);
        
        try {
            // Call Textract API
            AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                .document(Document.builder()
                    .bytes(SdkBytes.fromByteArray(documentContent))
                    .build())
                .featureTypes(FeatureType.TABLES, FeatureType.FORMS, FeatureType.LAYOUT)
                .build();
            
            AnalyzeDocumentResponse response = getClient().analyzeDocument(request);
            
            // Convert Textract blocks to DocumentChunks
            List<DocumentChunk> chunks = convertTextractBlocks(response.blocks(), documentFile);
            
            LOG.infof("Successfully parsed %s with Textract - %d chunks", 
                    documentFile.fileName, chunks.size());
            
            return chunks;
            
        } catch (Exception e) {
            LOG.errorf(e, "Textract parsing failed: %s", documentFile.fileName);
            throw new RuntimeException("Textract parsing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert Textract blocks to DocumentChunks.
     * Groups related blocks into semantic chunks.
     */
    private List<DocumentChunk> convertTextractBlocks(List<Block> blocks, DocumentFile documentFile) {
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder currentChunkContent = new StringBuilder();
        int chunkPosition = 0;
        int tokenCount = 0;
        
        for (Block block : blocks) {
            if (block.blockType() == BlockType.LINE) {
                // Add line text
                currentChunkContent.append(block.text()).append("\n");
                tokenCount += estimateTokens(block.text());
                
                // Create chunk if it exceeds size limit
                if (tokenCount > 500) {
                    chunks.add(createChunk(currentChunkContent.toString(), 
                                          chunkPosition++, 
                                          ContentType.TEXT, 
                                          tokenCount, 
                                          documentFile));
                    currentChunkContent.setLength(0);
                    tokenCount = 0;
                }
                
            } else if (block.blockType() == BlockType.TABLE) {
                // Handle table - convert to markdown
                String tableMarkdown = convertTableToMarkdown(block, blocks);
                chunks.add(createChunk(tableMarkdown, 
                                      chunkPosition++, 
                                      ContentType.TABLE, 
                                      estimateTokens(tableMarkdown), 
                                      documentFile));
            }
        }
        
        // Add remaining content as final chunk
        if (currentChunkContent.length() > 0) {
            chunks.add(createChunk(currentChunkContent.toString(), 
                                  chunkPosition, 
                                  ContentType.TEXT, 
                                  tokenCount, 
                                  documentFile));
        }
        
        return chunks;
    }
    
    private DocumentChunk createChunk(String content, int position, ContentType type, 
                                     int tokens, DocumentFile documentFile) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.documentFile = documentFile;
        chunk.position = position;
        chunk.content = content;
        chunk.contentType = type;
        chunk.tokenCount = tokens;
        return chunk;
    }
    
    private String convertTableToMarkdown(Block tableBlock, List<Block> allBlocks) {
        // Simplified table conversion
        // In production, you'd need to properly reconstruct the table structure
        StringBuilder markdown = new StringBuilder("| ");
        
        // Extract cells and build markdown table
        // This is a simplified version - real implementation would be more complex
        
        return markdown.toString();
    }
    
    private int estimateTokens(String text) {
        // Simple estimation: ~4 characters per token
        return Math.max(1, text.length() / 4);
    }
}
