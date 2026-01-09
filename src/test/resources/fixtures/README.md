# Test Fixtures

This directory contains test files and scripts for the PDF Processing Platform.

## Contents

### Python Script
- `generate_test_pdfs.py` - Generates sample PDFs and text files for testing

### Generated Test Files
The script creates the following test files:

#### Basic PDFs
- `simple.pdf` - Single page basic PDF
- `simple2.pdf` - Another basic PDF
- `test1.pdf`, `test2.pdf`, `test3.pdf` - Additional basic PDFs

#### Multi-page PDFs
- `multipage.pdf` - 5 pages of text content
- `text_only.pdf` - 3 pages of text
- `large.pdf` - 50 pages for performance testing

#### Specialized PDFs
- `with_images.pdf` - Contains graphical elements
- `with_tables.pdf` - Contains table data
- `metadata_test.pdf` - For metadata extraction/editing
- `encryption_test.pdf` - For encryption/decryption testing
- `watermark_test.pdf` - For watermark testing
- `comparison_test1.pdf` - First PDF for comparison
- `comparison_test2.pdf` - Second PDF for comparison
- `ocr_test.pdf` - Simulated scanned document

#### Conversion Sources
- `test_text.txt` - Text file for text-to-PDF conversion
- `test_html.html` - HTML file for HTML-to-PDF conversion
- `test_markdown.md` - Markdown file for markdown-to-PDF conversion

#### Simulated Office Documents
- `test_document.pdf` - Simulates Word document
- `test_spreadsheet.pdf` - Simulates Excel spreadsheet
- `test_presentation.pdf` - Simulates PowerPoint presentation

## Usage

### Generate Test Files
```bash
# Install required library
pip install reportlab

# Run the script
python generate_test_pdfs.py
```

### Test All Tools
Use these files to test all 32 tools:

1. **Merge**: `simple.pdf` + `simple2.pdf`
2. **Split**: `multipage.pdf`
3. **Compress**: `large.pdf`
4. **Rotate**: `simple.pdf`
5. **Watermark**: `watermark_test.pdf`
6. **Encrypt**: `encryption_test.pdf`
7. **Decrypt**: (encrypted file)
8. **Extract Text**: `text_only.pdf`
9. **Extract Images**: `with_images.pdf`
10. **Extract Metadata**: `metadata_test.pdf`
11. **Add Page Numbers**: `multipage.pdf`
12. **Remove Pages**: `multipage.pdf`
13. **Crop Pages**: `simple.pdf`
14. **Resize Pages**: `simple.pdf`
15. **Edit Metadata**: `metadata_test.pdf`
16. **Optimize**: `large.pdf`
17. **PDF to Image**: `simple.pdf`
18. **Image to PDF**: `with_images.pdf`
19. **PDF to Text**: `text_only.pdf`
20. **Text to PDF**: `test_text.txt`
21. **PDF to Word**: `test_document.pdf`
22. **PDF to Excel**: `test_spreadsheet.pdf`
23. **PDF to PPT**: `test_presentation.pdf`
24. **Word to PDF**: (requires .docx file)
25. **Excel to PDF**: (requires .xlsx file)
26. **PPT to PDF**: (requires .pptx file)
27. **HTML to PDF**: `test_html.html`
28. **Markdown to PDF**: `test_markdown.md`
29. **Text File to PDF**: `test_text.txt`
30. **PDF/A Convert**: `simple.pdf`
31. **OCR PDF**: `ocr_test.pdf`
32. **Compare PDFs**: `comparison_test1.pdf` + `comparison_test2.pdf`

## Test Data Structure

```
src/test/resources/fixtures/
├── generate_test_pdfs.py    # Generator script
├── README.md                # This file
└── test_files/              # Generated test files (created by script)
    ├── simple.pdf
    ├── simple2.pdf
    ├── multipage.pdf
    ├── text_only.pdf
    ├── large.pdf
    ├── with_images.pdf
    ├── with_tables.pdf
    ├── metadata_test.pdf
    ├── encryption_test.pdf
    ├── watermark_test.pdf
    ├── comparison_test1.pdf
    ├── comparison_test2.pdf
    ├── ocr_test.pdf
    ├── test_text.txt
    ├── test_html.html
    ├── test_markdown.md
    ├── test_document.pdf
    ├── test_spreadsheet.pdf
    ├── test_presentation.pdf
    ├── test1.pdf
    ├── test2.pdf
    └── test3.pdf
```

## Integration Testing

The generated files are used by:
- `PDFProcessingControllerIntegrationTest.java`
- `PDFUtilTest.java`
- `FileUtilTest.java`

## CI/CD Integration

Add to your CI pipeline:
```bash
cd src/test/resources/fixtures
pip install reportlab
python generate_test_pdfs.py
```

## Notes

- All PDFs are generated programmatically
- No external dependencies beyond reportlab
- Files are small for quick testing
- Suitable for unit and integration tests
- Can be extended for specific test scenarios