# PDF Processing Platform - Enterprise API Reference

**Version:** 2.0.0  
**Last Updated:** January 9, 2026  
**Base URL:** `http://localhost:8080/api`

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Rate Limiting](#rate-limiting)
4. [Response Format](#response-format)
5. [Error Handling](#error-handling)
6. [PDF Processing Endpoints](#pdf-processing-endpoints)
7. [Advanced Features](#advanced-features)
8. [Job Management](#job-management)
9. [Webhooks](#webhooks)
10. [File Download](#file-download)
11. [Health & Monitoring](#health--monitoring)
12. [SDKs & Examples](#sdks--examples)

---

## Overview

The PDF Processing Platform Enterprise API provides comprehensive PDF manipulation capabilities through a RESTful interface. All operations are processed asynchronously with job tracking.

### Key Features
- **60+ PDF Operations**: Merge, split, compress, convert, OCR, and more
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
                        │  (45+ Tools)    │
                        └─────────────────┘
                                │
                        ┌───────┴───────┐
                        ▼               ▼
                   ┌─────────┐    ┌─────────┐
                   │ Storage │    │ Webhook │
                   └─────────┘    └─────────┘
```

---

## Authentication

All API endpoints require authentication using one of the following methods:

### API Key (Recommended for Server-to-Server)

```http
X-API-Key: your-api-key-here
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/pdf/merge" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "files=@document1.pdf" \
  -F "files=@document2.pdf"
```

### JWT Token (For User Sessions)

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Default API Keys (Development)

| Key | Environment |
|-----|-------------|
| `demo-api-key-12345` | Development |
| `production-api-key-67890` | Production |

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

### Success Response
```json
{
  "success": true,
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Job submitted successfully",
  "timestamp": "2026-01-09T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "File size exceeds maximum limit",
    "details": {"maxSize": "500MB", "actualSize": "650MB"}
  },
  "timestamp": "2026-01-09T10:30:00Z"
}
```

---

## Error Handling

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid request parameters |
| `FILE_TOO_LARGE` | 413 | File exceeds size limit |
| `UNSUPPORTED_FORMAT` | 400 | File format not supported |
| `AUTHENTICATION_ERROR` | 401 | Invalid credentials |
| `JOB_NOT_FOUND` | 404 | Job ID not found |
| `PROCESSING_ERROR` | 500 | Processing failed |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |

---

## PDF Processing Endpoints

**Base Path:** `/api/pdf`

### 1. Merge PDFs

```http
POST /api/pdf/merge
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `files` | File[] | Yes | - | PDF files (2-50) |
| `outputFileName` | String | No | `merged_document` | Output name |
| `preserveBookmarks` | Boolean | No | `true` | Keep bookmarks |
| `removeAnnotations` | Boolean | No | `false` | Remove annotations |
| `mergeMode` | String | No | `sequential` | `sequential`, `interleave`, `interleave-reverse` |

**Example:**
```bash
curl -X POST "http://localhost:8080/api/pdf/merge" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf" \
  -F "mergeMode=interleave"
```

---

### 2. Split PDF

```http
POST /api/pdf/split
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `pagesPerFile` | Integer | No | `1` | Pages per file |
| `pageRanges` | String | No | - | Custom ranges: `1-3,5,7-9` |
| `outputPrefix` | String | No | `split_page` | Filename prefix |

---

### 3. Compress PDF

```http
POST /api/pdf/compress
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `compressionPreset` | String | No | `medium` | Preset level |
| `compressionQuality` | Double | No | `0.75` | Quality 0.1-1.0 |
| `removeMetadata` | Boolean | No | `false` | Strip metadata |
| `optimizeImages` | Boolean | No | `true` | Optimize images |
| `maxImageDpi` | Integer | No | `150` | Max image DPI |
| `subsetFonts` | Boolean | No | `true` | Subset fonts |
| `grayscaleImages` | Boolean | No | `false` | Grayscale conversion |

**Compression Presets:**

| Preset | Quality | DPI | Use Case |
|--------|---------|-----|----------|
| `low` | 0.95 | 300 | Minimal, max quality |
| `medium` | 0.75 | 150 | Balanced (default) |
| `high` | 0.50 | 100 | Aggressive |
| `extreme` | 0.30 | 72 | Maximum compression |
| `screen` | 0.60 | 96 | Web/screen |
| `print` | 0.85 | 300 | Print quality |
| `ebook` | 0.65 | 150 | E-reader |
| `archive` | 0.85 | 200 | Long-term storage |

---

### 4. Rotate PDF

```http
POST /api/pdf/rotate
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `angle` | Integer | Yes | - | `90`, `180`, `270` |
| `pageRange` | String | No | `all` | Pages to rotate |

---

### 5. Add Watermark

```http
POST /api/pdf/watermark
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `watermarkText` | String | Yes | - | Text |
| `fontName` | String | No | `Helvetica` | Font |
| `fontSize` | Integer | No | `48` | Size |
| `color` | String | No | `#808080` | Hex color |
| `opacity` | Double | No | `0.3` | Opacity 0-1 |
| `position` | String | No | `center` | Position |
| `rotation` | Integer | No | `45` | Angle |
| `diagonal` | Boolean | No | `true` | Diagonal |

---

### 6. Encrypt PDF

```http
POST /api/pdf/encrypt
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `ownerPassword` | String | Yes | - | Owner password |
| `userPassword` | String | No | - | User password |
| `allowPrint` | Boolean | No | `true` | Allow printing |
| `allowCopy` | Boolean | No | `true` | Allow copy |
| `allowModify` | Boolean | No | `true` | Allow modify |

---

### 7. Decrypt PDF

```http
POST /api/pdf/decrypt
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | Encrypted PDF |
| `password` | String | Yes | Password |

---

### 8. Extract Text

```http
POST /api/pdf/extract-text
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `pageRange` | String | No | `all` | Pages |
| `preserveLayout` | Boolean | No | `false` | Keep layout |

---

### 9. Extract Images

```http
POST /api/pdf/extract-images
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `format` | String | No | `png` | `png`, `jpg`, `tiff` |

---

### 10. Extract Metadata

```http
POST /api/pdf/extract-metadata
Content-Type: multipart/form-data
```

---

### 11. Add Page Numbers

```http
POST /api/pdf/add-page-numbers
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `position` | String | No | `bottom-center` | Position |
| `format` | String | No | `{page}` | Number format |
| `startPage` | Integer | No | `1` | First page |
| `startNumber` | Integer | No | `1` | Start number |
| `fontName` | String | No | `Helvetica` | Font |
| `fontSize` | Integer | No | `12` | Size |

---

### 12. Remove Pages

```http
POST /api/pdf/remove-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF file |
| `pageRange` | String | Yes | Pages: `1,3,5-7` |

---

### 13. Crop Pages

```http
POST /api/pdf/crop-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `marginTop` | Double | No | `0` | Top margin |
| `marginBottom` | Double | No | `0` | Bottom margin |
| `marginLeft` | Double | No | `0` | Left margin |
| `marginRight` | Double | No | `0` | Right margin |

---

### 14. Resize Pages

```http
POST /api/pdf/resize-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `pageSize` | String | No | - | `A4`, `Letter`, `Legal` |
| `width` | Double | No | - | Custom width |
| `height` | Double | No | - | Custom height |
| `scaleContent` | Boolean | No | `true` | Scale content |

---

### 15. PDF to Images

```http
POST /api/pdf/pdf-to-image
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `format` | String | No | `png` | `png`, `jpg`, `tiff` |
| `dpi` | Integer | No | `300` | Resolution |
| `pageRange` | String | No | `all` | Pages |

---

### 16. Images to PDF

```http
POST /api/pdf/image-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `files` | File[] | Yes | - | Image files |
| `pageSize` | String | No | `auto` | Page size |
| `margin` | Integer | No | `0` | Margin |

---

### 17. Office to PDF

```http
POST /api/pdf/office-to-pdf
Content-Type: multipart/form-data
```

**Supported:** `.doc`, `.docx`, `.xls`, `.xlsx`, `.ppt`, `.pptx`, `.odt`, `.ods`, `.odp`

---

### 18. HTML to PDF

```http
POST /api/pdf/html-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | HTML file |
| `pageSize` | String | No | `A4` | Page size |
| `landscape` | Boolean | No | `false` | Orientation |

---

### 19. Markdown to PDF

```http
POST /api/pdf/markdown-to-pdf
Content-Type: multipart/form-data
```

---

### 20. Text to PDF

```http
POST /api/pdf/text-to-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | Text file |
| `fontName` | String | No | `Courier` | Font |
| `fontSize` | Integer | No | `12` | Size |

---

### 21. TXT to PDF

```http
POST /api/pdf/txt-to-pdf
Content-Type: multipart/form-data
```

---

### 22. OCR PDF

```http
POST /api/pdf/ocr-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | Scanned PDF |
| `language` | String | No | `eng` | Language code |
| `dpi` | Integer | No | `300` | DPI |
| `outputType` | String | No | `searchable-pdf` | Output type |
| `enhanceScans` | Boolean | No | `true` | Enhance quality |

**Languages:** `eng`, `spa`, `fra`, `deu`, `ita`, `por`, `rus`, `chi_sim`, `chi_tra`, `jpn`, `kor`, `ara`, `hin` + 100 more

---

### 23. Compare PDFs

```http
POST /api/pdf/compare-pdfs
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file1` | File | Yes | - | First PDF |
| `file2` | File | Yes | - | Second PDF |
| `highlightColor` | String | No | `#FF0000` | Highlight color |
| `comparisonType` | String | No | `visual` | `visual`, `text` |

---

### 24. PDF/A Convert

```http
POST /api/pdf/pdfa-convert
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `conformanceLevel` | String | No | `PDFA_1B` | PDF/A level |
| `embedFonts` | Boolean | No | `true` | Embed fonts |

**Levels:** `PDFA_1A`, `PDFA_1B`, `PDFA_2A`, `PDFA_2B`, `PDFA_2U`, `PDFA_3A`, `PDFA_3B`, `PDFA_3U`

---

### 25. Linearize (Fast Web View)

```http
POST /api/pdf/linearize
Content-Type: multipart/form-data
```

---

### 26. Optimize

```http
POST /api/pdf/optimize
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `removeUnusedObjects` | Boolean | No | `true` | Remove unused |
| `compressStreams` | Boolean | No | `true` | Compress streams |
| `subsetFonts` | Boolean | No | `true` | Subset fonts |

---

### 27. Edit Metadata

```http
POST /api/pdf/metadata-edit
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF file |
| `title` | String | No | Title |
| `author` | String | No | Author |
| `subject` | String | No | Subject |
| `keywords` | String | No | Keywords |
| `creator` | String | No | Creator |
| `removeAll` | Boolean | No | Clear all |

---

### 28. Sign PDF

```http
POST /api/pdf/sign-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF file |
| `certificate` | File | Yes | PKCS#12 cert |
| `password` | String | Yes | Cert password |
| `reason` | String | No | Signing reason |
| `location` | String | No | Location |
| `visibleSignature` | Boolean | No | Show signature |
| `page` | Integer | No | Signature page |
| `x`, `y` | Integer | No | Position |
| `width`, `height` | Integer | No | Dimensions |

---

### 29. Verify Signature

```http
POST /api/pdf/verify-signature
Content-Type: multipart/form-data
```

**Response:**
```json
{
  "hasSignatures": true,
  "signatureCount": 2,
  "allValid": true,
  "signatures": [{
    "signerName": "John Doe",
    "signedAt": "2026-01-09T10:30:00Z",
    "isValid": true
  }]
}
```

---

### 30. Redact PDF

```http
POST /api/pdf/redact-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `searchTerms` | String | Yes | - | Terms to redact |
| `regex` | String | No | - | Regex pattern |
| `redactColor` | String | No | `#000000` | Redaction color |

---

### 31. Flatten PDF

```http
POST /api/pdf/flatten-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | PDF file |
| `flattenAnnotations` | Boolean | No | `true` | Flatten annotations |
| `flattenForms` | Boolean | No | `true` | Flatten forms |

---

### 32. Repair PDF

```http
POST /api/pdf/repair-pdf
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `file` | File | Yes | - | Corrupted PDF |
| `aggressive` | Boolean | No | `false` | Aggressive mode |

---

### 33. Reorder Pages

```http
POST /api/pdf/reorder-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF file |
| `order` | String | Yes | New order: `3,1,2,5,4` |

---

### 34. Insert Pages

```http
POST /api/pdf/insert-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | Main PDF |
| `insertFile` | File | Yes | PDF to insert |
| `position` | Integer | Yes | Insert after page |

---

### 35. Extract Pages

```http
POST /api/pdf/extract-pages
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | PDF file |
| `pageRange` | String | Yes | Pages: `1-5,10,15-20` |

---

## Advanced Features

**Base Path:** `/api/pdf/advanced`

### Form Management

#### Fill Form
```http
POST /api/pdf/advanced/forms/fill
Content-Type: multipart/form-data
```

**Request Body:**
```json
{
  "formData": {
    "fieldName1": "value1",
    "checkboxField": true
  },
  "flattenAfterFill": false
}
```

#### Extract Form Data
```http
POST /api/pdf/advanced/forms/extract
```

#### Create Form
```http
POST /api/pdf/advanced/forms/create
Content-Type: application/json
```

#### Detect Form Fields
```http
POST /api/pdf/advanced/forms/detect
```

---

### Annotations

#### Add Annotations
```http
POST /api/pdf/advanced/annotations/add
```

**Annotation Types:** `HIGHLIGHT`, `TEXT_NOTE`, `STAMP`, `UNDERLINE`, `STRIKEOUT`

**Stamp Types:** `APPROVED`, `REJECTED`, `DRAFT`, `FINAL`, `CONFIDENTIAL`, `VOID`

#### List Annotations
```http
POST /api/pdf/advanced/annotations/list
```

#### Remove Annotations
```http
POST /api/pdf/advanced/annotations/remove
```

#### Flatten Annotations
```http
POST /api/pdf/advanced/annotations/flatten
```

---

### AI Analysis

#### Analyze Document
```http
POST /api/pdf/advanced/ai/analyze
```

**Capabilities:** `SEMANTIC_SEARCH`, `DOCUMENT_QA`, `SUMMARIZE`, `EXTRACT_ENTITIES`, `CATEGORIZE`, `EXTRACT_KEY_POINTS`

#### Semantic Search
```http
POST /api/pdf/advanced/ai/search
```

#### Extract Entities
```http
POST /api/pdf/advanced/ai/entities
```

**Entity Types:** `PERSON`, `ORGANIZATION`, `LOCATION`, `DATE`, `MONEY`, `EMAIL`, `PHONE`

#### Summarize
```http
POST /api/pdf/advanced/ai/summarize
```

---

### Bookmarks

#### Add Bookmarks
```http
POST /api/pdf/advanced/bookmarks/add
```

#### Extract Bookmarks
```http
POST /api/pdf/advanced/bookmarks/extract
```

#### Remove Bookmarks
```http
POST /api/pdf/advanced/bookmarks/remove
```

#### Auto-Generate
```http
POST /api/pdf/advanced/bookmarks/auto-generate
```

---

### Validation

```http
POST /api/pdf/advanced/validate
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `checkPDFA` | Boolean | `false` | PDF/A check |
| `checkAccessibility` | Boolean | `false` | Accessibility |
| `checkSecurity` | Boolean | `true` | Security |
| `checkFonts` | Boolean | `true` | Font embedding |

---

## Job Management

### Get Job Status
```http
GET /api/pdf/jobs/{jobId}
```

**Status Values:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`

### List Jobs
```http
GET /api/pdf/jobs
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `status` | String | - | Filter by status |
| `page` | Integer | `0` | Page number |
| `size` | Integer | `20` | Page size |

### Cancel Job
```http
DELETE /api/pdf/jobs/{jobId}
```

### Get Statistics
```http
GET /api/pdf/stats
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
  "secret": "your-secret"
}
```

**Events:** `JOB_PENDING`, `JOB_PROCESSING`, `JOB_COMPLETED`, `JOB_FAILED`, `JOB_CANCELLED`

### List Webhooks
```http
GET /api/pdf/advanced/webhooks
```

### Unregister Webhook
```http
DELETE /api/pdf/advanced/webhooks/{webhookId}
```

### Webhook Payload
```json
{
  "event": "JOB_COMPLETED",
  "timestamp": "2026-01-09T10:30:45Z",
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "result": {
    "resultUrl": "/api/pdf/download/file.pdf"
  },
  "signature": "sha256=..."
}
```

---

## File Download

### Download Processed File
```http
GET /api/pdf/download/{filename}
```

### Download Job File
```http
GET /api/pdf/download/{jobId}/{filename}
```

---

## Health & Monitoring

### Health Check
```http
GET /api/actuator/health
```

### Application Info
```http
GET /api/actuator/info
```

### Metrics
```http
GET /api/actuator/metrics
GET /api/actuator/metrics/{metricName}
```

### Prometheus Metrics
```http
GET /api/actuator/prometheus
```

---

## SDKs & Examples

### cURL - Merge PDFs
```bash
curl -X POST "http://localhost:8080/api/pdf/merge" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "files=@doc1.pdf" \
  -F "files=@doc2.pdf"
```

### cURL - Compress with Preset
```bash
curl -X POST "http://localhost:8080/api/pdf/compress" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@large.pdf" \
  -F "compressionPreset=high"
```

### cURL - OCR
```bash
curl -X POST "http://localhost:8080/api/pdf/ocr-pdf" \
  -H "X-API-Key: demo-api-key-12345" \
  -F "file=@scanned.pdf" \
  -F "language=eng"
```

### Python Example
```python
import requests

API_URL = "http://localhost:8080/api/pdf"
API_KEY = "demo-api-key-12345"

def merge_pdfs(files, output="merged"):
    response = requests.post(
        f"{API_URL}/merge",
        headers={"X-API-Key": API_KEY},
        files=[('files', open(f, 'rb')) for f in files],
        data={"outputFileName": output}
    )
    return response.json()

result = merge_pdfs(["doc1.pdf", "doc2.pdf"])
print(f"Job ID: {result['jobId']}")
```

### JavaScript Example
```javascript
const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');

async function compressPDF(filePath, preset = 'medium') {
  const form = new FormData();
  form.append('file', fs.createReadStream(filePath));
  form.append('compressionPreset', preset);
  
  const response = await axios.post(
    'http://localhost:8080/api/pdf/compress',
    form,
    { headers: { ...form.getHeaders(), 'X-API-Key': 'demo-api-key-12345' } }
  );
  return response.data;
}
```

---

## Changelog

### Version 2.0.0 (January 2026)
- Added page interleave merge mode
- Added 8 compression presets
- Added batch scheduling service
- Added metadata sanitization
- Added PII removal and analysis
- 60+ total API endpoints

### Version 1.0.0
- Initial release with 32 tools
- Async job queue
- JWT/API Key authentication
- Webhook support
- AI document analysis

---

## Support

- **Documentation:** https://docs.pdfplatform.com
- **Support:** support@pdfplatform.com

---

© 2026 PDF Processing Platform Enterprise
