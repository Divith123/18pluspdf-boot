# Multi-stage build for optimized Docker image
FROM eclipse-temurin:21-jdk-jammy AS builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle files for dependency caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# FORCE REBUILD 2026-01-09 v3 - LibreOffice fix
# Install runtime dependencies for LibreOffice, Tesseract, MuPDF and more
RUN echo "BUILD_20260109_V3_LIBREOFFICE_FIX" && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    # LibreOffice for document conversion
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    # Tesseract OCR with multiple language packs
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-spa \
    tesseract-ocr-deu \
    tesseract-ocr-ita \
    tesseract-ocr-por \
    tesseract-ocr-rus \
    tesseract-ocr-jpn \
    tesseract-ocr-chi-sim \
    tesseract-ocr-chi-tra \
    tesseract-ocr-kor \
    tesseract-ocr-ara \
    tesseract-ocr-hin \
    tesseract-ocr-tur \
    tesseract-ocr-pol \
    tesseract-ocr-nld \
    tesseract-ocr-vie \
    tesseract-ocr-tha \
    tesseract-ocr-ind \
    # MuPDF tools for PDF manipulation
    mupdf-tools \
    # Ghostscript for PostScript/PDF processing
    ghostscript \
    # Poppler utilities (pdftotext, pdfimages, etc.)
    poppler-utils \
    # ImageMagick for image manipulation
    imagemagick \
    # Image optimization tools
    pngquant \
    jpegoptim \
    optipng \
    gifsicle \
    # FFmpeg for multimedia processing
    ffmpeg \
    # Fonts for better PDF rendering
    fonts-liberation \
    fonts-dejavu-core \
    fonts-freefont-ttf \
    fonts-noto-cjk \
    fonts-noto-color-emoji \
    # Utilities
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create non-root user
RUN groupadd -r pdfuser && useradd -r -g pdfuser pdfuser

# Create directories
RUN mkdir -p /app/data /app/logs /app/temp /app/output && \
    chown -R pdfuser:pdfuser /app

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set permissions
RUN chown pdfuser:pdfuser app.jar

# Switch to non-root user
USER pdfuser

# Environment variables
ENV JAVA_OPTS="-Xmx2g -Xms512m"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV APP_DATA_DIR="/app/data"
ENV APP_LOG_DIR="/app/logs"
ENV APP_TEMP_DIR="/app/temp"
ENV APP_OUTPUT_DIR="/app/output"
ENV LIBREOFFICE_PATH="/usr/bin/soffice"
ENV TESSERACT_PATH="/usr/bin/tesseract"
ENV MUPDF_PATH="/usr/bin/mutool"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels
LABEL maintainer="CHN India PDF Service" \
      version="1.0.0" \
      description="Enterprise PDF Processing Service with 32 Tools" \
      org.opencontainers.image.source="https://github.com/chnindia/pdf-service"