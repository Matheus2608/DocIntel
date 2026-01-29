# Test Fixtures for Docling Integration Tests

This directory contains test documents for validating Docling document processing.

## Required Fixtures

### test-pdf-with-tables.pdf
**Purpose**: Validate PDF table extraction and markdown conversion

**Requirements**:
- Multi-page PDF (2-3 pages)
- At least 2 complex tables with merged cells
- Mix of text paragraphs and tables
- Table with headers and footers

**Expected Output**: Markdown with valid table syntax

---

### test-docx-formatted.docx
**Purpose**: Validate DOCX hierarchy and formatting preservation

**Requirements**:
- Multiple heading levels (H1, H2, H3)
- Bullet and numbered lists
- Text formatting (bold, italic, links)
- At least 2 sections with clear hierarchy

**Expected Output**: Markdown preserving heading structure and lists

---

### test-multicolumn.pdf
**Purpose**: Validate complex layout handling

**Requirements**:
- Multi-column layout (2-3 columns)
- Reading order should be column-by-column
- Mix of text and images
- Complex page layout

**Expected Output**: Markdown with correct reading order

---

## Creating Test Fixtures

You can create these fixtures manually or use existing documents. For automated testing, consider:

1. **PDF with tables**: Export from Excel/Google Sheets with complex layouts
2. **DOCX formatted**: Create in Word/Google Docs with headings and lists
3. **Multi-column PDF**: Use newspaper/magazine PDFs or create from InDesign

## Placeholder Files

Until real fixtures are available, tests will skip or use minimal placeholders.
