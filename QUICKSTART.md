# Quick Start Guide

Get the PDF Processing Platform running in 5 minutes.

## Prerequisites

1. **Java 21** installed
2. **Gradle** (or use wrapper: `gradlew.bat`)
3. **LibreOffice** (for Office conversions)
4. **Tesseract OCR** (for OCR features)
5. **MuPDF tools** (for advanced PDF operations)

## Step 1: Install External Tools (Windows)

### LibreOffice
1. Download from: https://www.libreoffice.org/
2. Install to default location
3. Verify: `C:\Program Files\LibreOffice\program\soffice.exe`
4. Set environment variable:
```bash
set APP_EXTERNAL_TOOLS_LIBREOFFICE_PATH=C:\Program Files\LibreOffice\program\soffice.exe
```

### Tesseract OCR
1. Download from: https://github.com/UB-Mannheim/tesseract/wiki
2. Install to default location
3. Download language packs to: `C:\Program Files\Tesseract-OCR\tessdata\`
4. Verify: `C:\Program Files\Tesseract-OCR\tesseract.exe`
5. Set environment variables:
```bash
set APP_EXTERNAL_TOOLS_TESSERACT_PATH=C:\Program Files\Tesseract-OCR\tesseract.exe
set APP_EXTERNAL_TOOLS_TESSERACT_DATA_PATH=C:\Program Files\Tesseract-OCR\tessdata
```

### MuPDF Tools
1. Download from: https://mupdf.com/downloads
2. Extract to: `C:\Program Files\MuPDF\`
3. Verify: `C:\Program Files\MuPDF\mutool.exe`
4. Set environment variable:
```bash
set APP_EXTERNAL_TOOLS_MUPDF_PATH=C:\Program Files\MuPDF\mutool.exe
```

## Step 2: Configure Environment

### Option A: Environment Variables (Recommended)
```bash
# Security
set JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
set API_KEYS=admin-key-12345,service-key-67890

# External Tools
set APP_EXTERNAL_TOOLS_LIBREOFFICE_PATH=C:\Program Files\LibreOffice\program\soffice.exe
set APP_EXTERNAL_TOOLS_TESSERACT_PATH=C:\Program Files\Tesseract-OCR\tesseract.exe
set APP_EXTERNAL_TOOLS_TESSERACT_DATA_PATH=C:\Program Files\Tesseract-OCR\tessdata
set APP_EXTERNAL_TOOLS_MUPDF_PATH=C:\Program Files\MuPDF\mutool.exe

# File Storage
set APP_FILE_STORAGE_BASE_PATH=C:\NINJa\CHN\backend\data
set APP_FILE_STORAGE_TEMP_PATH=C:\NINJa\CHN\backend\temp
set APP_FILE_STORAGE_OUTPUT_PATH=C:\NINJa\CHN\backend\output
set APP_FILE_STORAGE_LOGS_PATH=C:\NINJa\CHN\backend\logs

# Profiles
set SPRING_PROFILES_ACTIVE=dev
```

### Option B: application.yaml
Edit `src/main/resources/application.yaml`:

```yaml
app:
  security:
    jwt:
      secret: "your-super-secret-jwt-key-change-this-in-production"
    api-keys:
      keys: "admin-key-12345,service-key-67890"
  
  external-tools:
    libreoffice-path: "C:/Program Files/LibreOffice/program/soffice.exe"
    tesseract-path: "C:/Program Files/Tesseract-OCR/tesseract.exe"
    tesseract-data-path: "C:/Program Files/Tesseract-OCR/tessdata"
    mupdf-path: "C:/Program Files/MuPDF/mutool.exe"
  
  file-storage:
    base-path: "./data"
    temp-path: "./temp"
    output-path: "./output"
    logs-path: "./logs"
```

## Step 3: Build and Run

### Option A: Use Startup Script (Recommended)
```bash
cd c:\NINJa\CHN\backend
start-dev.bat
```

### Option B: Manual Commands
```bash
cd c:\NINJa\CHN\backend

# Build
gradlew clean bootJar

# Run
gradlew bootRun

# Or run the JAR directly
java -jar build/libs/pdf-processing-platform-1.0.0.jar
```

### Option C: Docker (Easiest)
```bash
cd c:\NINJa\CHN\backend

# Create .env file
echo JWT_SECRET=your-secret-key > .env
echo API_KEYS=admin-key-12345 >> .env

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f pdf-processing
```

## Step 4: Verify Installation

### Check Health
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Test API Key
```bash
curl -X GET "http://localhost:8080/api/pdf/health" \
  -H "X-API-Key: admin-key-12345"
```

Expected response:
```json
{"status":"OK","message":"PDF Processing Platform is running"}
```

## Step 5: Test a Tool

### Test PDF Merge
```bash
# Create test PDFs first (or use existing ones)
# Then merge them
curl -X POST "http://localhost:8080/api/pdf/merge" \
  -H "X-API-Key: admin-key-12345" \
  -F "files=@document1.pdf" \
  -F "files=@document2.pdf" \
  -F "outputFileName=merged.pdf"
```

Expected response:
```json
{
  "jobId": "job-1234567890",
  "status": "PENDING",
  "message": "Job submitted successfully"
}
```

### Check Job Status
```bash
curl -X GET "http://localhost:8080/api/pdf/jobs/job-1234567890" \
  -H "X-API-Key: admin-key-12345"
```

### Download Result
```bash
curl -X GET "http://localhost:8080/api/download/job-1234567890" \
  -H "X-API-Key: admin-key-12345" \
  -o merged.pdf
```

## Step 6: Access Documentation

### Swagger UI
Open in browser: http://localhost:8080/swagger-ui.html

### API Reference
Open in browser: http://localhost:8080/api-docs

### Health & Metrics
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Info: http://localhost:8080/actuator/info

## Common Issues & Solutions

### Issue: "LibreOffice not found"
**Solution:**
```bash
# Verify installation
dir "C:\Program Files\LibreOffice\program\soffice.exe"

# Set correct path
set APP_EXTERNAL_TOOLS_LIBREOFFICE_PATH=C:\Program Files\LibreOffice\program\soffice.exe
```

### Issue: "Tesseract data not found"
**Solution:**
```bash
# Check tessdata directory
dir "C:\Program Files\Tesseract-OCR\tessdata\*.traineddata"

# Download missing language packs from:
# https://github.com/tesseract-ocr/tessdata
```

### Issue: "Port 8080 already in use"
**Solution:**
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID with actual PID)
taskkill /PID <PID> /F

# Or change port in application.yaml
server:
  port: 8081
```

### Issue: "Out of memory"
**Solution:**
```bash
# Increase JVM heap size
set JAVA_OPTS=-Xmx4g -Xms1g

# Or run with explicit memory
java -Xmx4g -jar build/libs/pdf-processing-platform-1.0.0.jar
```

### Issue: "File size limit exceeded"
**Solution:**
```yaml
# In application.yaml
app:
  file-storage:
    max-file-size: 1048576000  # 1GB
```

### Issue: "Docker container won't start"
**Solution:**
```bash
# Check logs
docker logs pdf-processing-platform

# Ensure directories exist
mkdir -p data temp output logs

# Check permissions
icacls data /grant "Everyone:F"
icacls temp /grant "Everyone:F"
```

## Development Mode

### Run with Hot Reload
```bash
# Using Spring Boot DevTools (automatic restart)
gradlew bootRun --continuous
```

### Run Tests
```bash
# All tests
gradlew test

# Unit tests only
gradlew test --tests "*Test"

# Integration tests only
gradlew test --tests "*IntegrationTest"

# With coverage report
gradlew test jacocoTestReport
```

### View Coverage Report
Open `build/reports/jacoco/test/html/index.html` in browser.

## Production Deployment

### Using Docker
```bash
# Build production image
docker build -t pdf-processing-platform:latest .

# Run with production profile
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=your-production-secret \
  -e API_KEYS=prod-key-12345 \
  -v ./data:/app/data \
  -v ./logs:/app/logs \
  --name pdf-platform \
  pdf-processing-platform:latest
```

### Using Docker Compose
```bash
# Create production .env file
echo JWT_SECRET=your-production-secret > .env
echo API_KEYS=prod-key-12345 >> .env
echo DB_PASSWORD=your-db-password >> .env

# Start all services
docker-compose -f docker-compose.yml up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Manual Deployment
```bash
# Build JAR
gradlew clean bootJar

# Run with production settings
java -Xmx4g -Xms1g \
  -Dspring.profiles.active=prod \
  -Djwt.secret=your-production-secret \
  -Dapi.keys=prod-key-12345 \
  -jar build/libs/pdf-processing-platform-1.0.0.jar
```

## Monitoring

### Check Application Logs
```bash
# Windows
type logs\application.log

# Or follow logs
powershell Get-Content logs/application.log -Wait
```

### Check Processing Logs
```bash
type logs\processing.log
```

### Check Error Logs
```bash
type logs\error.log
```

## Next Steps

1. **Read API Reference**: See [API_REFERENCE.md](API_REFERENCE.md) for complete API documentation
2. **Configure Security**: Set up proper JWT secrets and API keys
3. **Set up Database**: Configure PostgreSQL for production
4. **Monitor Performance**: Use actuator endpoints for monitoring
5. **Scale**: Configure thread pools and connection limits

## Support

If you encounter issues:
1. Check logs in `logs/` directory
2. Verify external tools are installed correctly
3. Check environment variables
4. Review [README.md](README.md) for detailed configuration
5. Open an issue on GitHub

## Quick Commands Reference

```bash
# Build
gradlew clean bootJar

# Run
gradlew bootRun

# Test
gradlew test

# Docker
docker-compose up -d
docker-compose logs -f
docker-compose down

# Check status
curl http://localhost:8080/actuator/health

# API with API Key
curl -H "X-API-Key: admin-key-12345" http://localhost:8080/api/pdf/health
```

# Run
gradlew bootRun
```

## Step 4: Verify Installation

Open browser or use curl:

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Should return: {"status":"UP"}
```

## Step 5: Test a Tool

### Test PDF Compression
```bash
# Create a test PDF first (or use any PDF)
# Then compress it:
curl -X POST http://localhost:8080/api/tools/compress \
  -F "file=@test.pdf" \
  -F "quality=0.75"
```

Response:
```json
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "toolName": "compress",
  "fileName": "test.pdf",
  "createdAt": "2026-01-09T10:00:00"
}
```

### Check Job Status
```bash
curl http://localhost:8080/api/tools/jobs/123e4567-e89b-12d3-a456-426614174000
```

### Download Result
```bash
curl -O http://localhost:8080/api/files/compressed_1234567890.pdf
```

## All Available Tools

### PDF Manipulation
- `/tools/merge` - Merge multiple PDFs
- `/tools/split` - Split by pages
- `/tools/compress` - Reduce file size
- `/tools/rotate` - Rotate pages
- `/tools/watermark` - Add watermark
- `/tools/encrypt` - Password protect
- `/tools/decrypt` - Remove password
- `/tools/extract-text` - Get text
- `/tools/extract-images` - Get images
- `/tools/extract-metadata` - Get metadata
- `/tools/add-page-numbers` - Number pages
- `/tools/remove-pages` - Delete pages
- `/tools/crop-pages` - Adjust margins
- `/tools/resize-pages` - Change dimensions

### Conversion
- `/tools/pdf-to-image` - PDF → PNG/JPG
- `/tools/image-to-pdf` - Image → PDF
- `/tools/pdf-to-text` - PDF → Text
- `/tools/pdf-to-word` - PDF → DOCX
- `/tools/pdf-to-excel` - PDF → XLSX
- `/tools/pdf-to-ppt` - PDF → PPTX
- `/tools/word-to-pdf` - DOCX → PDF
- `/tools/excel-to-pdf` - XLSX → PDF
- `/tools/ppt-to-pdf` - PPTX → PDF
- `/tools/html-to-pdf` - HTML → PDF
- `/tools/text-to-pdf` - Text → PDF

### OCR
- `/tools/ocr-extract` - Extract text from scans
- `/tools/ocr-searchable` - Make PDFs searchable

### Comparison
- `/tools/compare-pdfs` - Compare two PDFs
- `/tools/diff-highlight` - Highlight differences

### Utilities
- `/tools/repair-pdf` - Fix corrupted PDFs
- `/tools/optimize-pdf` - Optimize file size
- `/tools/flatten-forms` - Lock forms
- `/tools/remove-metadata` - Clean metadata

## Common Issues

### "LibreOffice not found"
Update path in `application.yaml`:
```yaml
app:
  external-tools:
    libreoffice-path: C:/Your/Path/soffice.exe
```

### "Tesseract not found"
Update path in `application.yaml`:
```yaml
app:
  external-tools:
    tesseract-path: C:/Your/Path/tesseract.exe
```

### "Port already in use"
Change port in `application.yaml`:
```yaml
server:
  port: 8081
```

### "Out of memory"
Increase heap size:
```bash
set JAVA_OPTS=-Xmx4g
gradlew bootRun
```

## Next Steps

1. **Read API_REFERENCE.md** for detailed endpoint docs
2. **Read DEPLOYMENT.md** for production deployment
3. **Test all 32 tools** with your PDFs
4. **Monitor logs** in `./logs/pdf-processing.log`
5. **Check health** at `http://localhost:8080/api/actuator/health`

## Production Deployment

For production, use Docker:

```bash
# Build
docker build -t pdf-processing-platform .

# Run
docker-compose up -d
```

## Support

- **Health Check**: `http://localhost:8080/api/actuator/health`
- **Metrics**: `http://localhost:8080/api/actuator/metrics`
- **Logs**: `./logs/pdf-processing.log`
- **API Docs**: `API_REFERENCE.md`

---

**You're all set!** The platform is ready to process PDFs. 🚀