# PDF Processing Platform - Enterprise API Reference

**Version:** 2.1.0  
**Last Updated:** January 9, 2026  
**Production URL:** `https://lobster-app-7qa89.ondigitalocean.app/api`  
**Local Development:** `http://localhost:8080/api`

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Rate Limiting](#rate-limiting)
4. [Response Format](#response-format)
5. [Error Codes](#error-codes)
6. [PDF Processing Endpoints](#pdf-processing-endpoints)
7. [Document Conversion Endpoints](#document-conversion-endpoints)
8. [Advanced Features](#advanced-features)
9. [Job Management](#job-management)
10. [Webhooks](#webhooks)
11. [File Download](#file-download)
12. [Health & Monitoring](#health--monitoring)
13. [Code Examples](#code-examples)
14. [Changelog](#changelog)

---

## Overview

The PDF Processing Platform Enterprise API provides comprehensive PDF manipulation capabilities through a RESTful interface. All operations are processed asynchronously with job tracking.

### Key Features

- **42+ PDF Operations**: Merge, split, compress, convert, OCR, and more
- **Async Processing**: All jobs are queued for reliable processing
- **Enterprise Security**: JWT + API Key authentication
- **Cloud Integration**: AWS S3, Google Cloud Storage, Azure Blob, Dropbox
- **AI-Powered**: Document analysis, entity extraction, semantic search
- **Webhook Notifications**: Real-time job status updates

### Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Client App    │────▶│   REST API      │────▶│   Job Queue     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                         │
                        ┌─────────────────┐              │
                        │   PDF Worker    │◀─────────────┘
                        │  (42+ Tools)    │
                        └─────────────────┘
                                │
                        ┌───────┴───────┐
                        ▼               ▼
                   ┌─────────┐    ┌─────────┐
                   │ Storage │    │ Webhook │
                   └─────────┘    └─────────┘
```

### Quick Start

```bash
# Health check (no auth required)
curl https://lobster-app-7qa89.ondigitalocean.app/api/actuator/health

# Compress a PDF
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/compress" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf"
```

---

## Authentication

All API endpoints (except health checks) require authentication.

### API Key Authentication (Recommended)

Include in request header:

```http
X-API-Key: your-api-key-here
```

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/compress" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf"
```

### JWT Token (For User Sessions)

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Available API Keys

| Key | Description |
|-----|-------------|
| `demo-api-key-12345` | Development/Testing |
| `admin-api-key-67890` | Admin access |

---

## Rate Limiting

| Tier | Requests/Minute | Concurrent Jobs | Max File Size |
|------|-----------------|-----------------|---------------|
| Free | 10 | 2 | 10 MB |
| Basic | 100 | 10 | 100 MB |
| Pro | 500 | 50 | 500 MB |
| Enterprise | Unlimited | Unlimited | 2 GB |

**Rate Limit Headers:**

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1704789600
```

---

## Response Format

### Success Response (Job Submitted)

```json
{
  "success": true,
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Job submitted successfully",
  "timestamp": "2026-01-09T10:30:00Z",
  "estimatedTime": "5-30 seconds"
}
```

### Job Status Response

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "progress": 100,
  "result": {
    "downloadUrl": "/api/pdf/download/550e8400-e29b-41d4-a716-446655440000/output.pdf",
    "fileSize": 1024567,
    "processingTime": 2345
  }
}
```

### Error Response

```json
{
  "timestamp": "2026-01-09T10:30:00Z",
  "errorCode": "VALIDATION_ERROR",
  "message": "File size exceeds maximum limit",
  "details": "Max size: 500MB, Actual size: 650MB"
}
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request parameters |
| `FILE_TOO_LARGE` | 413 | File exceeds size limit (500MB max) |
| `UNSUPPORTED_FORMAT` | 400 | File format not supported |
| `AUTHENTICATION_ERROR` | 401 | Invalid or missing API key |
| `JOB_NOT_FOUND` | 404 | Job ID not found |
| `PROCESSING_ERROR` | 500 | Processing failed |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## PDF Processing Endpoints

**Base Path:** `/api/pdf`

### 1. Merge PDFs

Combine multiple PDF files into a single document.

```http
POST /api/pdf/merge
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `files` | File[] | ✅ Yes | - | PDF files to merge (2-50 files) |
| `outputFileName` | String | No | `merged` | Output filename |
| `preserveBookmarks` | Boolean | No | `true` | Preserve bookmarks from source files |
| `removeAnnotations` | Boolean | No | `false` | Remove annotations |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/merge" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf" \
  -F "files=@doc3.pdf" \
  -F "outputFileName=merged_document"
```

---

### 2. Split PDF

Split a PDF into multiple files.

```http
POST /api/pdf/split
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file to split |
| `pagesPerFile` | Integer | No | `1` | Number of pages per output file |
| `pageRanges` | String | No | - | Custom ranges: `1-3,5,7-9` |
| `outputPrefix` | String | No | `split_page` | Output filename prefix |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/split" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf" \
  -F "pageRanges=1-5,10-15"
```

---

### 3. Compress PDF

Reduce PDF file size with various compression levels.

```http
POST /api/pdf/compress
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file to compress |
| `compressionPreset` | String | No | `medium` | Compression preset |
| `compressionQuality` | Double | No | `0.75` | Quality (0.1-1.0) |
| `removeMetadata` | Boolean | No | `false` | Strip metadata |
| `optimizeImages` | Boolean | No | `true` | Optimize embedded images |
| `maxImageDpi` | Integer | No | `150` | Maximum image DPI |
| `grayscaleImages` | Boolean | No | `false` | Convert images to grayscale |

**Compression Presets:**

| Preset | Quality | DPI | Best For |
|--------|---------|-----|----------|
| `low` | 0.95 | 300 | Minimal compression, max quality |
| `medium` | 0.75 | 150 | Balanced (recommended) |
| `high` | 0.50 | 100 | Aggressive compression |
| `extreme` | 0.30 | 72 | Maximum compression |
| `screen` | 0.60 | 96 | Web/screen viewing |
| `print` | 0.85 | 300 | Print quality |
| `ebook` | 0.65 | 150 | E-readers |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/compress" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@large_document.pdf" \
  -F "compressionPreset=high"
```

---

### 4. Rotate PDF

Rotate PDF pages by specified angle.

```http
POST /api/pdf/rotate
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `angle` | Integer | ✅ Yes | - | Rotation angle: `90`, `180`, `270` |
| `pageRange` | String | No | `all` | Pages to rotate: `1,3,5-7` |

---

### 5. Add Watermark

Add text watermark to PDF pages.

```http
POST /api/pdf/watermark
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `watermarkText` | String | ✅ Yes | - | Watermark text |
| `fontName` | String | No | `Helvetica` | Font name |
| `fontSize` | Integer | No | `48` | Font size |
| `color` | String | No | `#808080` | Hex color code |
| `opacity` | Double | No | `0.3` | Opacity (0.0-1.0) |
| `position` | String | No | `center` | Position on page |
| `rotation` | Integer | No | `45` | Rotation angle |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/watermark" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf" \
  -F "watermarkText=CONFIDENTIAL" \
  -F "opacity=0.5"
```

---

### 6. Encrypt PDF

Add password protection to PDF.

```http
POST /api/pdf/encrypt
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `ownerPassword` | String | ✅ Yes | - | Owner/admin password |
| `userPassword` | String | No | - | User password (to open) |
| `allowPrint` | Boolean | No | `true` | Allow printing |
| `allowCopy` | Boolean | No | `true` | Allow copy/paste |
| `allowModify` | Boolean | No | `true` | Allow modifications |

---

### 7. Decrypt PDF

Remove password protection from PDF.

```http
POST /api/pdf/decrypt
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | Encrypted PDF file |
| `password` | String | ✅ Yes | PDF password |

---

### 8. Extract Text

Extract text content from PDF.

```http
POST /api/pdf/extract-text
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `pageRange` | String | No | `all` | Pages to extract |
| `preserveLayout` | Boolean | No | `false` | Preserve text layout |

---

### 9. Extract Images

Extract all images from PDF.

```http
POST /api/pdf/extract-images
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `format` | String | No | `png` | Output format: `png`, `jpg`, `tiff` |

---

### 10. Extract Metadata

Get PDF document metadata.

```http
POST /api/pdf/extract-metadata
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |

---

### 11. Add Page Numbers

Add page numbers to PDF.

```http
POST /api/pdf/add-page-numbers
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `position` | String | No | `bottom-center` | Number position |
| `format` | String | No | `{page}` | Format: `{page}`, `Page {page} of {total}` |
| `startPage` | Integer | No | `1` | First page to number |
| `startNumber` | Integer | No | `1` | Starting number |
| `fontName` | String | No | `Helvetica` | Font name |
| `fontSize` | Integer | No | `12` | Font size |

---

### 12. Remove Pages

Remove specific pages from PDF.

```http
POST /api/pdf/remove-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `pageRange` | String | ✅ Yes | Pages to remove: `1,3,5-7` |

---

### 13. Crop Pages

Crop PDF page margins.

```http
POST /api/pdf/crop-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `marginTop` | Double | No | `0` | Top margin (points) |
| `marginBottom` | Double | No | `0` | Bottom margin (points) |
| `marginLeft` | Double | No | `0` | Left margin (points) |
| `marginRight` | Double | No | `0` | Right margin (points) |

---

### 14. Resize Pages

Resize PDF pages to different dimensions.

```http
POST /api/pdf/resize-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `pageSize` | String | No | - | Standard size: `A4`, `Letter`, `Legal`, `A3` |
| `width` | Double | No | - | Custom width (points) |
| `height` | Double | No | - | Custom height (points) |
| `scaleContent` | Boolean | No | `true` | Scale content to fit |

---

### 15. PDF to Images

Convert PDF pages to images.

```http
POST /api/pdf/pdf-to-image
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `imageFormat` | String | No | `png` | Format: `png`, `jpg`, `tiff` |
| `dpi` | Integer | No | `300` | Resolution (72-600) |
| `pageRange` | String | No | `all` | Pages to convert |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-image" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf" \
  -F "imageFormat=png" \
  -F "dpi=150"
```

---

### 16. Images to PDF

Convert images to PDF document.

```http
POST /api/pdf/image-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `images` | File[] | ✅ Yes | - | Image files (PNG, JPG, TIFF) |
| `pageSize` | String | No | `A4` | Page size |
| `fitToPage` | Boolean | No | `true` | Fit image to page |
| `margin` | Integer | No | `10` | Page margin (points) |

---

### 17. OCR PDF

Perform OCR on scanned PDF to make it searchable.

```http
POST /api/pdf/ocr-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Scanned PDF file |
| `language` | String | No | `eng` | OCR language code |
| `dpi` | Integer | No | `300` | Processing DPI |
| `makeSearchable` | Boolean | No | `true` | Create searchable PDF |

**Supported Languages:**

`eng` (English), `spa` (Spanish), `fra` (French), `deu` (German), `ita` (Italian), `por` (Portuguese), `rus` (Russian), `chi_sim` (Chinese Simplified), `chi_tra` (Chinese Traditional), `jpn` (Japanese), `kor` (Korean), `ara` (Arabic), `hin` (Hindi), + 100 more

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/ocr-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@scanned_document.pdf" \
  -F "language=eng"
```

---

### 18. Compare PDFs

Compare two PDF documents and highlight differences.

```http
POST /api/pdf/compare-pdfs
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file1` | File | ✅ Yes | - | First PDF |
| `file2` | File | ✅ Yes | - | Second PDF |
| `highlightColor` | String | No | `#FF0000` | Difference highlight color |

---

### 19. PDF/A Convert

Convert PDF to PDF/A archival format.

```http
POST /api/pdf/pdfa-convert
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `conformanceLevel` | String | No | `PDFA_1B` | PDF/A conformance level |
| `embedFonts` | Boolean | No | `true` | Embed all fonts |

**Conformance Levels:** `PDFA_1A`, `PDFA_1B`, `PDFA_2A`, `PDFA_2B`, `PDFA_2U`, `PDFA_3A`, `PDFA_3B`, `PDFA_3U`

---

### 20. Linearize PDF

Optimize PDF for fast web viewing.

```http
POST /api/pdf/linearize
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |

---

### 21. Optimize PDF

Optimize PDF for reduced file size.

```http
POST /api/pdf/optimize
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `removeUnusedObjects` | Boolean | No | `true` | Remove unused objects |
| `compressStreams` | Boolean | No | `true` | Compress content streams |
| `subsetFonts` | Boolean | No | `true` | Subset embedded fonts |

---

### 22. Edit Metadata

Modify PDF document metadata.

```http
POST /api/pdf/metadata-edit
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `title` | String | No | Document title |
| `author` | String | No | Author name |
| `subject` | String | No | Subject |
| `keywords` | String | No | Comma-separated keywords |
| `creator` | String | No | Creator application |
| `removeAll` | Boolean | No | Clear all metadata |

---

### 23. Sign PDF

Digitally sign PDF with certificate.

```http
POST /api/pdf/sign-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `certificate` | File | ✅ Yes | - | PKCS#12 certificate (.p12/.pfx) |
| `password` | String | ✅ Yes | - | Certificate password |
| `reason` | String | No | - | Signing reason |
| `location` | String | No | - | Signing location |
| `visibleSignature` | Boolean | No | `false` | Show visible signature |
| `page` | Integer | No | `1` | Signature page |
| `x` | Integer | No | `50` | X position |
| `y` | Integer | No | `50` | Y position |
| `width` | Integer | No | `200` | Signature width |
| `height` | Integer | No | `50` | Signature height |

---

### 24. Verify Signature

Verify digital signatures in PDF.

```http
POST /api/pdf/verify-signature
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | Signed PDF file |

**Response:**

```json
{
  "hasSignatures": true,
  "signatureCount": 2,
  "allValid": true,
  "signatures": [
    {
      "signerName": "John Doe",
      "signedAt": "2026-01-09T10:30:00Z",
      "isValid": true,
      "reason": "Approved",
      "location": "New York"
    }
  ]
}
```

---

### 25. Redact PDF

Permanently redact sensitive content.

```http
POST /api/pdf/redact-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `searchTerms` | String | ✅ Yes | - | Comma-separated terms to redact |
| `regex` | String | No | - | Regex pattern for redaction |
| `redactColor` | String | No | `#000000` | Redaction box color |

---

### 26. Flatten PDF

Flatten annotations and form fields.

```http
POST /api/pdf/flatten-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `flattenAnnotations` | Boolean | No | `true` | Flatten annotations |
| `flattenForms` | Boolean | No | `true` | Flatten form fields |

---

### 27. Repair PDF

Attempt to repair corrupted PDF.

```http
POST /api/pdf/repair-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Corrupted PDF file |
| `aggressive` | Boolean | No | `false` | Use aggressive repair mode |

---

### 28. Reorder Pages

Rearrange PDF pages.

```http
POST /api/pdf/reorder-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `order` | String | ✅ Yes | New page order: `3,1,2,5,4` |

---

### 29. Insert Pages

Insert pages from one PDF into another.

```http
POST /api/pdf/insert-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | Main PDF file |
| `insertFile` | File | ✅ Yes | PDF to insert |
| `position` | Integer | ✅ Yes | Insert after this page (0 = beginning) |

---

### 30. Extract Pages

Extract specific pages into new PDF.

```http
POST /api/pdf/extract-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `pageRange` | String | ✅ Yes | Pages to extract: `1-5,10,15-20` |

---

## Document Conversion Endpoints

**Base Path:** `/api/pdf`

### 31. PDF to Word (DOCX)

Convert PDF to Microsoft Word document.

```http
POST /api/pdf/pdf-to-word
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `pageRange` | String | No | `all` | Pages to convert |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-word" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf"
```

---

### 32. PDF to Excel (XLSX)

Convert PDF tables to Excel spreadsheet.

```http
POST /api/pdf/pdf-to-excel
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file with tables |
| `pageRange` | String | No | `all` | Pages to convert |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-excel" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@spreadsheet.pdf"
```

---

### 33. PDF to PowerPoint (PPTX)

Convert PDF to PowerPoint presentation.

```http
POST /api/pdf/pdf-to-ppt
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `pageRange` | String | No | `all` | Pages to convert |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-ppt" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@presentation.pdf"
```

---

### 34. Word to PDF

Convert Word document to PDF.

```http
POST /api/pdf/word-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Word file (.doc, .docx) |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/word-to-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.docx"
```

---

### 35. Excel to PDF

Convert Excel spreadsheet to PDF.

```http
POST /api/pdf/excel-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Excel file (.xls, .xlsx) |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/excel-to-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@spreadsheet.xlsx"
```

---

### 36. PowerPoint to PDF

Convert PowerPoint presentation to PDF.

```http
POST /api/pdf/ppt-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PowerPoint file (.ppt, .pptx) |
| `outputFileName` | String | No | `converted` | Output filename |

**Example:**

```bash
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/ppt-to-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@presentation.pptx"
```

---

### 37. Office to PDF

Convert any Office document to PDF (auto-detect format).

```http
POST /api/pdf/office-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Office file |
| `outputFileName` | String | No | - | Output filename |
| `hideComments` | Boolean | No | `false` | Hide document comments |
| `exportBookmarks` | Boolean | No | `true` | Export bookmarks |

**Supported Formats:** `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx`, `.odt`, `.ods`, `.odp`

---

### 38. HTML to PDF

Convert HTML file to PDF.

```http
POST /api/pdf/html-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | HTML file |
| `pageSize` | String | No | `A4` | Page size |
| `margin` | Integer | No | `20` | Page margin |
| `enableImages` | Boolean | No | `true` | Include images |

---

### 39. Markdown to PDF

Convert Markdown file to PDF.

```http
POST /api/pdf/markdown-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Markdown file (.md) |
| `pageSize` | String | No | `A4` | Page size |
| `theme` | String | No | `default` | Styling theme |
| `enableCodeHighlighting` | Boolean | No | `true` | Syntax highlighting |

---

### 40. Text to PDF

Convert plain text file to PDF.

```http
POST /api/pdf/text-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | Text file |
| `fontName` | String | No | `Helvetica` | Font name |
| `fontSize` | Integer | No | `12` | Font size |
| `pageSize` | String | No | `A4` | Page size |
| `margin` | Integer | No | `36` | Page margin |

---

## Advanced Features

**Base Path:** `/api/pdf/advanced`

### Form Operations

#### Fill PDF Form

```http
POST /api/pdf/advanced/forms/fill
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF form file |
| `data` | JSON | ✅ Yes | Form field data `{"fieldName": "value"}` |
| `flattenAfterFill` | Boolean | No | Flatten form after filling |

#### Extract Form Data

```http
POST /api/pdf/advanced/forms/extract
Content-Type: multipart/form-data
```

#### Detect Form Fields

```http
POST /api/pdf/advanced/forms/detect
Content-Type: multipart/form-data
```

---

### Annotations

#### Add Annotations

```http
POST /api/pdf/advanced/annotations/add
Content-Type: multipart/form-data
```

**Annotation Types:** `HIGHLIGHT`, `TEXT_NOTE`, `STAMP`, `UNDERLINE`, `STRIKEOUT`

**Stamp Types:** `APPROVED`, `REJECTED`, `DRAFT`, `FINAL`, `CONFIDENTIAL`, `VOID`

#### List Annotations

```http
POST /api/pdf/advanced/annotations/list
Content-Type: multipart/form-data
```

#### Remove Annotations

```http
POST /api/pdf/advanced/annotations/remove
Content-Type: multipart/form-data
```

---

### AI Analysis

#### Analyze Document

```http
POST /api/pdf/advanced/ai/analyze
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `enableSentiment` | Boolean | No | Analyze sentiment |
| `enableTopics` | Boolean | No | Extract topics |

#### Semantic Search

```http
POST /api/pdf/advanced/ai/search
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | ✅ Yes | PDF file |
| `query` | String | ✅ Yes | Search query |
| `topK` | Integer | No | Number of results |

#### Extract Entities

```http
POST /api/pdf/advanced/ai/entities
Content-Type: multipart/form-data
```

**Entity Types:** `PERSON`, `ORGANIZATION`, `LOCATION`, `DATE`, `MONEY`, `EMAIL`, `PHONE`

#### Summarize Document

```http
POST /api/pdf/advanced/ai/summarize
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | ✅ Yes | - | PDF file |
| `maxLength` | Integer | No | `500` | Max summary length |
| `language` | String | No | `en` | Output language |

---

### Bookmarks

#### Add Bookmarks

```http
POST /api/pdf/advanced/bookmarks/add
Content-Type: multipart/form-data
```

#### Extract Bookmarks

```http
POST /api/pdf/advanced/bookmarks/extract
Content-Type: multipart/form-data
```

#### Auto-Generate Bookmarks

```http
POST /api/pdf/advanced/bookmarks/auto-generate
Content-Type: multipart/form-data
```

---

### Document Validation

```http
POST /api/pdf/advanced/validate
Content-Type: multipart/form-data
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `file` | File | - | PDF file |
| `checkPDFA` | Boolean | `false` | Validate PDF/A compliance |
| `checkAccessibility` | Boolean | `false` | Check accessibility |
| `checkSecurity` | Boolean | `true` | Check security settings |
| `checkFonts` | Boolean | `true` | Check font embedding |

---

## Job Management

### Get Job Status

```http
GET /api/pdf/jobs/{jobId}
```

**Response:**

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "progress": 100,
  "toolName": "compress",
  "createdAt": "2026-01-09T10:30:00Z",
  "completedAt": "2026-01-09T10:30:05Z",
  "result": {
    "downloadUrl": "/api/pdf/download/550e8400.../compressed.pdf",
    "originalSize": 5242880,
    "compressedSize": 1048576,
    "compressionRatio": 80
  }
}
```

**Status Values:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`

---

### List All Jobs

```http
GET /api/pdf/jobs
GET /api/pdf/jobs?status=COMPLETED
```

---

### Cancel Job

```http
DELETE /api/pdf/jobs/{jobId}
```

---

### Get Statistics

```http
GET /api/pdf/stats
```

**Response:**

```json
{
  "totalJobs": 1250,
  "completedJobs": 1200,
  "failedJobs": 25,
  "pendingJobs": 25,
  "averageProcessingTime": 3500,
  "popularTools": ["compress", "merge", "pdf-to-excel"]
}
```

---

## Webhooks

### Register Webhook

```http
POST /api/pdf/advanced/webhooks/register
Content-Type: application/json
```

```json
{
  "url": "https://your-app.com/webhook",
  "events": ["JOB_COMPLETED", "JOB_FAILED"],
  "secret": "your-webhook-secret"
}
```

**Events:** `JOB_PENDING`, `JOB_PROCESSING`, `JOB_COMPLETED`, `JOB_FAILED`, `JOB_CANCELLED`

---

### Webhook Payload

```json
{
  "event": "JOB_COMPLETED",
  "timestamp": "2026-01-09T10:30:45Z",
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "result": {
    "downloadUrl": "/api/pdf/download/file.pdf"
  },
  "signature": "sha256=abc123..."
}
```

---

## File Download

### Download Result File

```http
GET /api/pdf/download/{jobId}/{filename}
```

**Example:**

```bash
curl -O -H "X-API-Key: demo-api-key-12345" \
  "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/download/550e8400.../output.pdf"
```

---

## Health & Monitoring

### Health Check (No Auth Required)

```http
GET /api/actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

### Application Info

```http
GET /api/actuator/info
```

---

## Code Examples

### JavaScript (Fetch API)

```javascript
const API_URL = 'https://lobster-app-7qa89.ondigitalocean.app/api/pdf';
const API_KEY = 'demo-api-key-12345';

// Compress PDF
async function compressPDF(file) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('compressionPreset', 'high');

  const response = await fetch(`${API_URL}/compress`, {
    method: 'POST',
    headers: { 'X-API-Key': API_KEY },
    body: formData
  });
  
  return response.json();
}

// Check job status
async function getJobStatus(jobId) {
  const response = await fetch(`${API_URL}/jobs/${jobId}`, {
    headers: { 'X-API-Key': API_KEY }
  });
  return response.json();
}

// Poll until complete
async function waitForJob(jobId) {
  while (true) {
    const status = await getJobStatus(jobId);
    if (['COMPLETED', 'FAILED'].includes(status.status)) {
      return status;
    }
    await new Promise(r => setTimeout(r, 1000));
  }
}

// Usage
const fileInput = document.querySelector('input[type="file"]');
fileInput.addEventListener('change', async (e) => {
  const result = await compressPDF(e.target.files[0]);
  console.log('Job ID:', result.jobId);
  
  const status = await waitForJob(result.jobId);
  console.log('Download URL:', status.result?.downloadUrl);
});
```

---

### JavaScript (Node.js)

```javascript
const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');

const API_URL = 'https://lobster-app-7qa89.ondigitalocean.app/api/pdf';
const API_KEY = 'demo-api-key-12345';

async function pdfToExcel(filePath) {
  const form = new FormData();
  form.append('file', fs.createReadStream(filePath));

  const response = await axios.post(`${API_URL}/pdf-to-excel`, form, {
    headers: {
      ...form.getHeaders(),
      'X-API-Key': API_KEY
    }
  });
  return response.data;
}

async function getJobStatus(jobId) {
  const response = await axios.get(`${API_URL}/jobs/${jobId}`, {
    headers: { 'X-API-Key': API_KEY }
  });
  return response.data;
}

async function downloadFile(url, outputPath) {
  const response = await axios.get(
    `https://lobster-app-7qa89.ondigitalocean.app${url}`,
    { headers: { 'X-API-Key': API_KEY }, responseType: 'stream' }
  );
  response.data.pipe(fs.createWriteStream(outputPath));
}

// Usage
(async () => {
  const result = await pdfToExcel('spreadsheet.pdf');
  console.log('Job ID:', result.jobId);

  // Poll for completion
  let status;
  do {
    await new Promise(r => setTimeout(r, 1000));
    status = await getJobStatus(result.jobId);
  } while (!['COMPLETED', 'FAILED'].includes(status.status));

  if (status.status === 'COMPLETED') {
    await downloadFile(status.result.downloadUrl, 'output.xlsx');
    console.log('Downloaded: output.xlsx');
  }
})();
```

---

### Python

```python
import requests
import time

API_URL = "https://lobster-app-7qa89.ondigitalocean.app/api/pdf"
API_KEY = "demo-api-key-12345"

headers = {"X-API-Key": API_KEY}

def pdf_to_excel(file_path):
    """Convert PDF to Excel"""
    with open(file_path, "rb") as f:
        response = requests.post(
            f"{API_URL}/pdf-to-excel",
            headers=headers,
            files={"file": f}
        )
    return response.json()

def get_job_status(job_id):
    """Get job status"""
    response = requests.get(f"{API_URL}/jobs/{job_id}", headers=headers)
    return response.json()

def wait_for_job(job_id, timeout=60):
    """Wait for job completion"""
    start = time.time()
    while time.time() - start < timeout:
        status = get_job_status(job_id)
        if status["status"] in ["COMPLETED", "FAILED"]:
            return status
        time.sleep(1)
    raise TimeoutError("Job timed out")

def download_result(download_url, output_path):
    """Download result file"""
    response = requests.get(
        f"https://lobster-app-7qa89.ondigitalocean.app{download_url}",
        headers=headers
    )
    with open(output_path, "wb") as f:
        f.write(response.content)

# Usage
result = pdf_to_excel("spreadsheet.pdf")
print(f"Job ID: {result['jobId']}")

status = wait_for_job(result["jobId"])
if status["status"] == "COMPLETED":
    download_result(status["result"]["downloadUrl"], "output.xlsx")
    print("Done!")
```

---

### cURL Examples

```bash
# Health check
curl https://lobster-app-7qa89.ondigitalocean.app/api/actuator/health

# Compress PDF
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/compress" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf" \
  -F "compressionPreset=high"

# PDF to Excel
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-excel" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@spreadsheet.pdf"

# PDF to Word
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/pdf-to-word" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf"

# Merge PDFs
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/merge" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf"

# Check job status
curl "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/jobs/YOUR_JOB_ID" \
  -H "X-API-Key: demo-api-key-12345"

# OCR scanned document
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/ocr-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@scanned.pdf" \
  -F "language=eng"

# Add watermark
curl -X POST "https://lobster-app-7qa89.ondigitalocean.app/api/pdf/watermark" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@document.pdf" \
  -F "watermarkText=CONFIDENTIAL"
```

---

## Endpoint Summary Table

| # | Endpoint | Method | Description |
|---|----------|--------|-------------|
| 1 | `/api/pdf/merge` | POST | Merge multiple PDFs |
| 2 | `/api/pdf/split` | POST | Split PDF into parts |
| 3 | `/api/pdf/compress` | POST | Compress PDF |
| 4 | `/api/pdf/rotate` | POST | Rotate pages |
| 5 | `/api/pdf/watermark` | POST | Add watermark |
| 6 | `/api/pdf/encrypt` | POST | Password protect |
| 7 | `/api/pdf/decrypt` | POST | Remove password |
| 8 | `/api/pdf/extract-text` | POST | Extract text |
| 9 | `/api/pdf/extract-images` | POST | Extract images |
| 10 | `/api/pdf/extract-metadata` | POST | Get metadata |
| 11 | `/api/pdf/add-page-numbers` | POST | Add page numbers |
| 12 | `/api/pdf/remove-pages` | POST | Remove pages |
| 13 | `/api/pdf/crop-pages` | POST | Crop pages |
| 14 | `/api/pdf/resize-pages` | POST | Resize pages |
| 15 | `/api/pdf/pdf-to-image` | POST | PDF to images |
| 16 | `/api/pdf/image-to-pdf` | POST | Images to PDF |
| 17 | `/api/pdf/ocr-pdf` | POST | OCR scanned PDF |
| 18 | `/api/pdf/compare-pdfs` | POST | Compare two PDFs |
| 19 | `/api/pdf/pdfa-convert` | POST | Convert to PDF/A |
| 20 | `/api/pdf/linearize` | POST | Optimize for web |
| 21 | `/api/pdf/optimize` | POST | Optimize PDF |
| 22 | `/api/pdf/metadata-edit` | POST | Edit metadata |
| 23 | `/api/pdf/sign-pdf` | POST | Digital signature |
| 24 | `/api/pdf/verify-signature` | POST | Verify signatures |
| 25 | `/api/pdf/redact-pdf` | POST | Redact content |
| 26 | `/api/pdf/flatten-pdf` | POST | Flatten annotations |
| 27 | `/api/pdf/repair-pdf` | POST | Repair PDF |
| 28 | `/api/pdf/reorder-pages` | POST | Reorder pages |
| 29 | `/api/pdf/insert-pages` | POST | Insert pages |
| 30 | `/api/pdf/extract-pages` | POST | Extract pages |
| 31 | `/api/pdf/pdf-to-word` | POST | **PDF to Word (DOCX)** |
| 32 | `/api/pdf/pdf-to-excel` | POST | **PDF to Excel (XLSX)** |
| 33 | `/api/pdf/pdf-to-ppt` | POST | **PDF to PowerPoint (PPTX)** |
| 34 | `/api/pdf/word-to-pdf` | POST | **Word to PDF** |
| 35 | `/api/pdf/excel-to-pdf` | POST | **Excel to PDF** |
| 36 | `/api/pdf/ppt-to-pdf` | POST | **PowerPoint to PDF** |
| 37 | `/api/pdf/office-to-pdf` | POST | Office to PDF |
| 38 | `/api/pdf/html-to-pdf` | POST | HTML to PDF |
| 39 | `/api/pdf/markdown-to-pdf` | POST | Markdown to PDF |
| 40 | `/api/pdf/text-to-pdf` | POST | Text to PDF |
| 41 | `/api/pdf/jobs/{id}` | GET | Job status |
| 42 | `/api/pdf/jobs` | GET | List jobs |
| 43 | `/api/pdf/stats` | GET | Statistics |
| 44 | `/api/actuator/health` | GET | Health check |

---

## Changelog

### Version 2.1.0 (January 9, 2026)

**New Features:**
- ✅ Added PDF to Word conversion (`/pdf-to-word`)
- ✅ Added PDF to Excel conversion (`/pdf-to-excel`)
- ✅ Added PDF to PowerPoint conversion (`/pdf-to-ppt`)
- ✅ Added Word to PDF conversion (`/word-to-pdf`)
- ✅ Added Excel to PDF conversion (`/excel-to-pdf`)
- ✅ Added PowerPoint to PDF conversion (`/ppt-to-pdf`)

**Fixes:**
- Fixed endpoint routing issues
- Updated SpringDoc OpenAPI to 2.8.6 for Spring Boot 3.5.9 compatibility

### Version 2.0.0 (January 2026)

- Added compression presets
- Added batch scheduling service
- Added AI document analysis
- Added webhook notifications
- 40 total API endpoints

### Version 1.0.0

- Initial release with 32 tools
- Async job queue
- JWT/API Key authentication

---

## Support

- **API Status:** https://lobster-app-7qa89.ondigitalocean.app/api/actuator/health
- **Swagger UI:** https://lobster-app-7qa89.ondigitalocean.app/api/swagger-ui.html
- **OpenAPI Spec:** https://lobster-app-7qa89.ondigitalocean.app/api/v3/api-docs

---

© 2026 PDF Processing Platform Enterprise
