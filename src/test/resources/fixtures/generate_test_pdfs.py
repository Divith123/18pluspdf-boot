#!/usr/bin/env python3
"""
Test PDF Generator for PDF Processing Platform
Generates sample PDF files for testing all 32 tools
"""

import os
import sys
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.units import inch, cm
from reportlab.lib.colors import red, blue, green, black
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, Table, TableStyle
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
import io

def create_sample_pdf(filename, pages=1, content_type="text"):
    """Create a sample PDF file"""
    doc = SimpleDocTemplate(filename, pagesize=letter)
    story = []
    styles = getSampleStyleSheet()
    
    # Title
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        alignment=TA_CENTER,
        textColor=blue,
        spaceAfter=30
    )
    story.append(Paragraph(f"Test PDF - {content_type.title()}", title_style))
    story.append(Spacer(1, 0.2*inch))
    
    # Content based on type
    if content_type == "text":
        # Text content
        body_style = ParagraphStyle(
            'Body',
            parent=styles['BodyText'],
            alignment=TA_JUSTIFY,
            spaceAfter=12
        )
        for i in range(pages):
            story.append(Paragraph(
                f"This is page {i+1} of the test document. "
                f"This PDF contains sample text content that can be used for "
                f"extracting text, OCR testing, and other text-based operations. "
                f"The quick brown fox jumps over the lazy dog. "
                f"Numbers: 1234567890. Special chars: !@#$%^&*()",
                body_style
            ))
            if i < pages - 1:
                story.append(Spacer(1, 0.5*inch))
    
    elif content_type == "image":
        # Create a simple image using ReportLab
        from reportlab.graphics.shapes import Drawing, Rect
        from reportlab.graphics import renderPDF
        
        # Add some text
        story.append(Paragraph("PDF with Embedded Images", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        
        # Add a simple drawing (simulated image)
        drawing = Drawing(400, 200)
        drawing.add(Rect(50, 50, 300, 100, fillColor=red, strokeColor=black, strokeWidth=2))
        drawing.add(Rect(100, 75, 200, 50, fillColor=blue, strokeColor=black, strokeWidth=2))
        story.append(drawing)
        story.append(Spacer(1, 0.3*inch))
        
        story.append(Paragraph("This PDF contains graphical elements for image extraction testing.", styles['BodyText']))
    
    elif content_type == "table":
        # Table content
        story.append(Paragraph("PDF with Tables", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        
        data = [
            ['ID', 'Name', 'Value', 'Status'],
            ['1', 'Item A', '100', 'Active'],
            ['2', 'Item B', '200', 'Inactive'],
            ['3', 'Item C', '300', 'Active'],
            ['4', 'Item D', '400', 'Pending']
        ]
        
        table = Table(data)
        table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), green),
            ('TEXTCOLOR', (0, 0), (-1, 0), black),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 12),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('BACKGROUND', (0, 1), (-1, -1), white),
            ('GRID', (0, 0), (-1, -1), 1, black)
        ]))
        story.append(table)
        story.append(Spacer(1, 0.3*inch))
        
        story.append(Paragraph("This PDF contains tables for extraction and conversion testing.", styles['BodyText']))
    
    elif content_type == "metadata":
        # Metadata test
        story.append(Paragraph("PDF with Metadata", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        story.append(Paragraph(
            "This PDF has custom metadata that can be extracted and edited. "
            "Author: Test Author, Title: Test Metadata PDF, Subject: Testing",
            styles['BodyText']
        ))
    
    elif content_type == "encrypted":
        # Encrypted test (content will be encrypted later)
        story.append(Paragraph("PDF for Encryption Testing", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        story.append(Paragraph(
            "This PDF will be encrypted with a password for testing decryption.",
            styles['BodyText']
        ))
        for i in range(pages):
            story.append(Paragraph(f"Content for encryption test - page {i+1}", styles['BodyText']))
    
    elif content_type == "watermark":
        # Watermark test
        story.append(Paragraph("PDF for Watermark Testing", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        story.append(Paragraph(
            "This PDF will have a watermark added for testing.",
            styles['BodyText']
        ))
        for i in range(pages):
            story.append(Paragraph(f"Content page {i+1} - watermark will be added", styles['BodyText']))
    
    elif content_type == "comparison":
        # Comparison test
        story.append(Paragraph("PDF for Comparison Testing", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        story.append(Paragraph(
            "This PDF will be compared with another similar PDF.",
            styles['BodyText']
        ))
        for i in range(pages):
            story.append(Paragraph(f"Comparison content - page {i+1}", styles['BodyText']))
    
    elif content_type == "ocr":
        # OCR test (simulated scanned document)
        story.append(Paragraph("Simulated Scanned Document", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        story.append(Paragraph(
            "This PDF simulates a scanned document for OCR testing. "
            "In real scenarios, this would be an image-based PDF.",
            styles['BodyText']
        ))
        # Add text that OCR should recognize
        story.append(Spacer(1, 0.5*inch))
        story.append(Paragraph("OCR TEST TEXT:", styles['Heading3']))
        story.append(Paragraph("The quick brown fox jumps over the lazy dog", styles['BodyText']))
        story.append(Paragraph("1234567890", styles['BodyText']))
        story.append(Paragraph("ABCDEFGHIJKLMNOPQRSTUVWXYZ", styles['BodyText']))
        story.append(Paragraph("abcdefghijklmnopqrstuvwxyz", styles['BodyText']))
    
    elif content_type == "large":
        # Large PDF for performance testing
        story.append(Paragraph("Large PDF for Performance Testing", styles['Heading2']))
        story.append(Spacer(1, 0.2*inch))
        for i in range(pages):
            story.append(Paragraph(f"Page {i+1} of {pages}", styles['Heading3']))
            for j in range(20):
                story.append(Paragraph(
                    f"Line {j+1}: This is a long line of text to fill up the PDF and make it larger for performance testing. "
                    f"The quick brown fox jumps over the lazy dog. Numbers: {i*1000 + j}.",
                    styles['BodyText']
                ))
            if i < pages - 1:
                story.append(Spacer(1, 0.5*inch))
    
    # Build the PDF
    doc.build(story)
    print(f"Created: {filename} ({pages} pages, type: {content_type})")

def create_simple_pdf(filename, content="Test Content"):
    """Create a very simple PDF for basic testing"""
    c = canvas.Canvas(filename, pagesize=letter)
    c.setFont("Helvetica", 12)
    c.drawString(1*inch, 10*inch, "Simple Test PDF")
    c.setFont("Helvetica", 10)
    c.drawString(1*inch, 9.5*inch, content)
    c.drawString(1*inch, 9.0*inch, "This is a minimal PDF for basic testing.")
    c.save()
    print(f"Created simple PDF: {filename}")

def create_text_file(filename):
    """Create a text file for text-to-PDF conversion"""
    with open(filename, 'w') as f:
        f.write("Test Text File for PDF Conversion\n")
        f.write("=" * 40 + "\n\n")
        f.write("This is a test text file that will be converted to PDF.\n")
        f.write("It contains multiple lines of text.\n\n")
        f.write("Line 1: The quick brown fox jumps over the lazy dog.\n")
        f.write("Line 2: Numbers: 1234567890\n")
        f.write("Line 3: Special chars: !@#$%^&*()\n")
        f.write("Line 4: ABCDEFGHIJKLMNOPQRSTUVWXYZ\n")
        f.write("Line 5: abcdefghijklmnopqrstuvwxyz\n\n")
        f.write("This file can be used to test the text-to-PDF conversion tool.\n")
    print(f"Created text file: {filename}")

def create_html_file(filename):
    """Create an HTML file for HTML-to-PDF conversion"""
    html_content = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Test HTML for PDF Conversion</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #2c3e50; border-bottom: 2px solid #3498db; }
        h2 { color: #34495e; margin-top: 30px; }
        p { line-height: 1.6; color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #3498db; color: white; }
        .highlight { background-color: #f1c40f; padding: 2px 4px; }
    </style>
</head>
<body>
    <h1>Test HTML Document</h1>
    <p>This HTML file will be converted to PDF using the HTML-to-PDF tool.</p>
    
    <h2>Text Content</h2>
    <p>The quick brown fox jumps over the lazy dog. This is a standard pangram used for testing fonts and text rendering.</p>
    
    <h2>Formatted Text</h2>
    <p>This is <strong>bold text</strong>, this is <em>italic text</em>, and this is <span class="highlight">highlighted text</span>.</p>
    
    <h2>Table Example</h2>
    <table>
        <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Value</th>
        </tr>
        <tr>
            <td>1</td>
            <td>Item A</td>
            <td>100</td>
        </tr>
        <tr>
            <td>2</td>
            <td>Item B</td>
            <td>200</td>
        </tr>
        <tr>
            <td>3</td>
            <td>Item C</td>
            <td>300</td>
        </tr>
    </table>
    
    <h2>List Example</h2>
    <ul>
        <li>First item in unordered list</li>
        <li>Second item in unordered list</li>
        <li>Third item in unordered list</li>
    </ul>
    
    <ol>
        <li>First item in ordered list</li>
        <li>Second item in ordered list</li>
        <li>Third item in ordered list</li>
    </ol>
    
    <h2>Code Block</h2>
    <pre><code>function test() {
    console.log("Hello, World!");
    return true;
}</code></pre>
    
    <p><strong>End of HTML document.</strong></p>
</body>
</html>
"""
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(html_content)
    print(f"Created HTML file: {filename}")

def create_markdown_file(filename):
    """Create a Markdown file for Markdown-to-PDF conversion"""
    md_content = """# Test Markdown Document

This Markdown file will be converted to PDF using the Markdown-to-PDF tool.

## Text Content

The quick brown fox jumps over the lazy dog. This is a standard pangram used for testing.

## Formatted Text

- **Bold text**
- *Italic text*
- ~~Strikethrough text~~
- `Inline code`

## Lists

### Unordered List
- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
- Item 3

### Ordered List
1. First item
2. Second item
   1. Nested item 2.1
   2. Nested item 2.2
3. Third item

## Code Block

```python
def hello_world():
    print("Hello, World!")
    return True
```

## Table

| ID | Name | Value | Status |
|----|------|-------|--------|
| 1 | Item A | 100 | Active |
| 2 | Item B | 200 | Inactive |
| 3 | Item C | 300 | Active |

## Blockquote

> This is a blockquote.
> It can span multiple lines.
> Used for quoting text.

## Horizontal Rule

---

## Links and Images

[Example Link](https://example.com)

*Note: Images won't be rendered in PDF conversion, but links will be preserved.*

## End of Document

This concludes the test Markdown document.
"""
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(md_content)
    print(f"Created Markdown file: {filename}")

def create_word_like_document():
    """Create a simple document that simulates Word content"""
    # This creates a PDF that looks like a Word document
    # For actual Word conversion testing, you'd need python-docx
    filename = "test_document.pdf"
    doc = SimpleDocTemplate(filename, pagesize=letter)
    story = []
    styles = getSampleStyleSheet()
    
    # Professional document style
    story.append(Paragraph("Business Document Template", styles['Title']))
    story.append(Spacer(1, 0.3*inch))
    
    story.append(Paragraph("Executive Summary", styles['Heading1']))
    story.append(Paragraph(
        "This document represents a typical business document that would be "
        "created in Microsoft Word and converted to PDF format.",
        styles['BodyText']
    ))
    story.append(Spacer(1, 0.2*inch))
    
    story.append(Paragraph("Section 1: Introduction", styles['Heading2']))
    story.append(Paragraph(
        "The purpose of this document is to test the Word-to-PDF conversion "
        "capabilities of the PDF Processing Platform.",
        styles['BodyText']
    ))
    story.append(Spacer(1, 0.2*inch))
    
    story.append(Paragraph("Section 2: Requirements", styles['Heading2']))
    story.append(Paragraph(
        "1. Test all conversion tools\n"
        "2. Verify output quality\n"
        "3. Measure processing time\n"
        "4. Validate metadata preservation",
        styles['BodyText']
    ))
    story.append(Spacer(1, 0.2*inch))
    
    story.append(Paragraph("Section 3: Conclusion", styles['Heading2']))
    story.append(Paragraph(
        "This document successfully demonstrates the conversion capabilities.",
        styles['BodyText']
    ))
    
    doc.build(story)
    print(f"Created Word-like document: {filename}")

def create_excel_like_document():
    """Create a simple document that simulates Excel content"""
    filename = "test_spreadsheet.pdf"
    doc = SimpleDocTemplate(filename, pagesize=letter)
    story = []
    styles = getSampleStyleSheet()
    
    story.append(Paragraph("Spreadsheet Data Template", styles['Title']))
    story.append(Spacer(1, 0.3*inch))
    
    # Create a table that looks like spreadsheet data
    data = [
        ['Product', 'Q1', 'Q2', 'Q3', 'Q4', 'Total'],
        ['Widget A', '100', '120', '110', '130', '460'],
        ['Widget B', '200', '210', '220', '230', '860'],
        ['Widget C', '150', '160', '170', '180', '660'],
        ['Widget D', '300', '310', '320', '330', '1260'],
        ['Total', '750', '800', '820', '870', '3240']
    ]
    
    table = Table(data)
    table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), (0.2, 0.4, 0.8)),
        ('TEXTCOLOR', (0, 0), (-1, 0), (1, 1, 1)),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, 0), 10),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), (0.95, 0.95, 0.95)),
        ('GRID', (0, 0), (-1, -1), 1, (0.5, 0.5, 0.5)),
        ('FONTNAME', (0, 1), (-1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 1), (-1, -1), 9)
    ]))
    
    story.append(table)
    story.append(Spacer(1, 0.3*inch))
    
    story.append(Paragraph("This PDF contains spreadsheet-style data for conversion testing.", styles['BodyText']))
    
    doc.build(story)
    print(f"Created Excel-like document: {filename}")

def create_ppt_like_document():
    """Create a simple document that simulates PowerPoint content"""
    filename = "test_presentation.pdf"
    doc = SimpleDocTemplate(filename, pagesize=letter)
    story = []
    styles = getSampleStyleSheet()
    
    # Slide 1
    story.append(Paragraph("Presentation Title Slide", styles['Title']))
    story.append(Spacer(1, 0.5*inch))
    story.append(Paragraph("PDF Processing Platform Demo", styles['Heading1']))
    story.append(Paragraph("Version 1.0", styles['BodyText']))
    story.append(Spacer(1, 1*inch))
    
    # Slide 2
    story.append(Paragraph("Slide 2: Features", styles['Heading2']))
    story.append(Spacer(1, 0.2*inch))
    story.append(Paragraph("• 32 PDF Tools\n• Async Processing\n• REST API\n• Docker Support\n• OpenAPI Documentation", styles['BodyText']))
    story.append(Spacer(1, 1*inch))
    
    # Slide 3
    story.append(Paragraph("Slide 3: Technology Stack", styles['Heading2']))
    story.append(Spacer(1, 0.2*inch))
    story.append(Paragraph("• Spring Boot 3.5.9\n• Java 21\n• Apache PDFBox\n• OpenPDF\n• Tess4J\n• LibreOffice", styles['BodyText']))
    
    doc.build(story)
    print(f"Created PowerPoint-like document: {filename}")

def main():
    """Generate all test files"""
    # Create output directory
    output_dir = "test_files"
    os.makedirs(output_dir, exist_ok=True)
    os.chdir(output_dir)
    
    print("=" * 60)
    print("Generating Test Files for PDF Processing Platform")
    print("=" * 60)
    
    # Basic PDFs
    create_simple_pdf("simple.pdf", "Basic test content")
    create_simple_pdf("simple2.pdf", "Another basic test")
    
    # Multi-page PDFs
    create_sample_pdf("multipage.pdf", pages=5, content_type="text")
    create_sample_pdf("text_only.pdf", pages=3, content_type="text")
    
    # Specialized PDFs
    create_sample_pdf("with_images.pdf", pages=2, content_type="image")
    create_sample_pdf("with_tables.pdf", pages=2, content_type="table")
    create_sample_pdf("metadata_test.pdf", pages=1, content_type="metadata")
    create_sample_pdf("encryption_test.pdf", pages=2, content_type="encrypted")
    create_sample_pdf("watermark_test.pdf", pages=2, content_type="watermark")
    create_sample_pdf("comparison_test1.pdf", pages=3, content_type="comparison")
    create_sample_pdf("comparison_test2.pdf", pages=3, content_type="comparison")
    create_sample_pdf("ocr_test.pdf", pages=2, content_type="ocr")
    
    # Large PDF for performance testing
    create_sample_pdf("large.pdf", pages=50, content_type="large")
    
    # Conversion source files
    create_text_file("test_text.txt")
    create_html_file("test_html.html")
    create_markdown_file("test_markdown.md")
    
    # Simulated Office documents
    create_word_like_document()
    create_excel_like_document()
    create_ppt_like_document()
    
    # Additional test files
    create_simple_pdf("test1.pdf", "Test file 1")
    create_simple_pdf("test2.pdf", "Test file 2")
    create_simple_pdf("test3.pdf", "Test file 3")
    
    print("\n" + "=" * 60)
    print("Test files generated successfully!")
    print("=" * 60)
    print("\nFiles created:")
    for file in sorted(os.listdir()):
        size = os.path.getsize(file)
        print(f"  {file:30s} ({size:,} bytes)")
    
    print(f"\nTotal files: {len(os.listdir())}")
    print(f"Total size: {sum(os.path.getsize(f) for f in os.listdir()):,} bytes")
    print("\nYou can now use these files to test the PDF Processing Platform.")

if __name__ == "__main__":
    try:
        main()
    except ImportError as e:
        print(f"Error: Missing required library - {e}")
        print("\nPlease install reportlab:")
        print("  pip install reportlab")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)