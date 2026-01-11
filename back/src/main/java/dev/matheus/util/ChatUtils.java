package dev.matheus.util;

import com.google.gson.Gson;
import dev.langchain4j.data.document.*;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Utility class for document parsing and question extraction.
 */
public class ChatUtils {

    private static final Gson gson = new Gson();

    private ChatUtils() {
        // Utility class
    }

    public static int getNumberQuestions(String chunk) {
        return QuestionCountCalculator.calculateOptimalQuestionCount(chunk);
    }

    public static Document toDocument(byte[] docBytes, String contentType, String fileName) {
        InputStream inputStream = new ByteArrayInputStream(docBytes);
        DocumentParser parser = selectParser(contentType, fileName);
        Document document = parser.parse(inputStream);

        Metadata metadata = document.metadata();
        metadata.put("file_name", fileName);
        metadata.put("content_type", contentType);

        return document;
    }

    static DocumentParser selectParser(String contentType, String fileName) {
        if (isPdf(contentType, fileName)) {
            return new ApachePdfBoxDocumentParser();
        } else if (isDocx(contentType, fileName)) {
            return new ApachePoiDocumentParser();
        } else {
            return new TextDocumentParser();
        }
    }

    public static boolean isPdf(String contentType, String fileName) {
        return (contentType != null && contentType.contains("pdf")) ||
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    private static boolean isDocx(String contentType, String fileName) {
        return (contentType != null &&
                (contentType.contains("wordprocessingml") ||
                        contentType.contains("msword"))) ||
                (fileName != null &&
                        (fileName.toLowerCase().endsWith(".docx") ||
                                fileName.toLowerCase().endsWith(".doc")));
    }

    private static boolean isText(String contentType, String fileName) {
        return (contentType != null && contentType.contains("text")) ||
                (fileName != null && fileName.toLowerCase().endsWith(".txt"));
    }

    public static String[] toCleanedQuestions(String responseText) {
        String cleanedText = responseText.strip();
        if (cleanedText.startsWith("```json")) {
            cleanedText = cleanedText.substring(7).strip();
        }
        if (cleanedText.endsWith("```")) {
            cleanedText = cleanedText.substring(0, cleanedText.length() - 3).strip();
        }
        return gson.fromJson(cleanedText, String[].class);
    }

}
