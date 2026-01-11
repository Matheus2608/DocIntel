package dev.matheus.service.pdf;

import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

import java.util.List;

/**
 * Converts Tabula tables to Markdown format with proper cleaning.
 */
public class TableToMarkdownConverter {

    private TableToMarkdownConverter() {
        // Utility class
    }

    public static String convert(Table table) {
        var rows = table.getRows();

        if (rows == null || rows.size() < 2) {
            return "";
        }

        StringBuilder md = new StringBuilder();
        md.append("\n[START_TABLE]\n");

        for (int i = 0; i < rows.size(); i++) {
            appendRow(md, rows.get(i));

            if (i == 0) {
                appendHeaderSeparator(md, rows.get(i).size());
            }
        }

        md.append("[END_TABLE]\n");
        return md.toString();
    }

    private static void appendRow(StringBuilder md, List<RectangularTextContainer> row) {
        md.append("| ");
        for (RectangularTextContainer<?> cell : row) {
            String content = cleanCellContent(cell.getText());
            md.append(content.isEmpty() ? " " : content).append(" | ");
        }
        md.append("\n");
    }

    private static void appendHeaderSeparator(StringBuilder md, int columnCount) {
        md.append("|");
        md.append(" --- |".repeat(columnCount));
        md.append("\n");
    }

    private static String cleanCellContent(String content) {
        String cleaned = content
                .replaceAll("[\\r\\n]+", " ")
                .replace("|", "\\|")
                .trim();

        // Remove standalone page numbers (1-3 digits)
        if (cleaned.matches("^\\d{1,3}$")) {
            return "";
        }

        return cleaned;
    }
}

