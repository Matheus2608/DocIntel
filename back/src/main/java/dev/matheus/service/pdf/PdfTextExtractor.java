package dev.matheus.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import technology.tabula.Rectangle;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom PDF text stripper that tags table regions with markers for later removal.
 * This allows us to extract clean text by marking table areas with [[LIXO_INICIO]] and [[LIXO_FIM]].
 */
public class PdfTextExtractor extends PDFTextStripper {
    private final Map<Integer, List<Rectangle>> tableRegionsByPage;
    private boolean isInsideTable = false;

    public PdfTextExtractor(Map<Integer, List<Rectangle>> tableRegionsByPage) throws IOException {
        super();
        this.tableRegionsByPage = tableRegionsByPage;
        this.setSortByPosition(true);
        this.setSpacingTolerance(1.0f);
        this.setAverageCharTolerance(2.0f);
    }

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        if (textPositions.isEmpty()) {
            super.writeString(string, textPositions);
            return;
        }

        int page = this.getCurrentPageNo();
        TextPosition pos = textPositions.getFirst();
        float x = pos.getXDirAdj();
        float y = pos.getYDirAdj();

        boolean currentlyInTable = isPositionInTable(page, x, y);

        if (currentlyInTable && !isInsideTable) {
            output.write("[[LIXO_INICIO]]");
            isInsideTable = true;
        } else if (!currentlyInTable && isInsideTable) {
            output.write("[[LIXO_FIM]]");
            isInsideTable = false;
        }

        super.writeString(string, textPositions);
    }

    private boolean isPositionInTable(int page, float x, float y) {
        List<Rectangle> regions = tableRegionsByPage.get(page);
        if (regions == null) {
            return false;
        }

        return regions.stream().anyMatch(r ->
                y >= (r.getTop() - 3) && y <= (r.getBottom() + 3) &&
                x >= (r.getLeft() - 3) && x <= (r.getRight() + 3)
        );
    }
}

