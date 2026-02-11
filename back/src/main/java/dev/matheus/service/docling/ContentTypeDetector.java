package dev.matheus.service.docling;

import dev.matheus.entity.ContentType;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.regex.Pattern;

/**
 * Detects content type from markdown text.
 * Analyzes markdown content and classifies it into appropriate ContentType.
 */
@ApplicationScoped
public class ContentTypeDetector {

    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("\\|[-:| ]+\\|");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);
    private static final Pattern LIST_PATTERN = Pattern.compile("^[\\-*+]\\s+", Pattern.MULTILINE);
    private static final Pattern CODE_PATTERN = Pattern.compile("```");

    /**
     * Detect content type based on markdown content analysis.
     * Returns MIXED if multiple content types are detected.
     *
     * @param content The markdown content to analyze
     * @return The detected ContentType
     */
    public ContentType detect(String content) {
        if (content == null || content.isEmpty()) {
            return ContentType.TEXT;
        }

        boolean hasTable = detectTable(content);
        boolean hasHeading = detectHeading(content);
        boolean hasList = detectList(content);
        boolean hasCode = detectCode(content);

        // Count content types and determine primary type
        int typeCount = 0;
        ContentType primaryType = ContentType.TEXT;

        if (hasTable) {
            typeCount++;
            primaryType = ContentType.TABLE;
        }
        if (hasHeading) {
            typeCount++;
            if (primaryType == ContentType.TEXT) {
                primaryType = ContentType.HEADING;
            }
        }
        if (hasList) {
            typeCount++;
            if (primaryType == ContentType.TEXT) {
                primaryType = ContentType.LIST;
            }
        }
        if (hasCode) {
            typeCount++;
            if (primaryType == ContentType.TEXT) {
                primaryType = ContentType.CODE;
            }
        }

        // If multiple content types detected, mark as MIXED
        return typeCount > 1 ? ContentType.MIXED : primaryType;
    }

    private boolean detectTable(String content) {
        return content.contains("|") && TABLE_SEPARATOR_PATTERN.matcher(content).find();
    }

    private boolean detectHeading(String content) {
        return HEADING_PATTERN.matcher(content).find();
    }

    private boolean detectList(String content) {
        return LIST_PATTERN.matcher(content).find();
    }

    private boolean detectCode(String content) {
        return CODE_PATTERN.matcher(content).find();
    }
}
