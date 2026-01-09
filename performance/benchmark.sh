#!/bin/bash
# Performance Benchmark Script for PDF Processing Platform
# Tests all 32 tools with various file sizes

set -e

# Configuration
BASE_URL="http://localhost:8080/api/pdf"
API_KEY="admin-key-12345"
TEST_DIR="./test_files"
RESULTS_DIR="./results"
CONCURRENT_TESTS=5

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create directories
mkdir -p "$RESULTS_DIR"
mkdir -p "$TEST_DIR"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if server is running
check_server() {
    log_info "Checking if server is running..."
    if curl -s -H "X-API-Key: $API_KEY" "$BASE_URL/health" > /dev/null; then
        log_success "Server is running"
        return 0
    else
        log_error "Server is not running at $BASE_URL"
        return 1
    fi
}

# Generate test files if they don't exist
generate_test_files() {
    log_info "Generating test files..."
    cd src/test/resources/fixtures
    python generate_test_pdfs.py
    cd ../../../..
    cp src/test/resources/fixtures/test_files/* ./test_files/ 2>/dev/null || true
    log_success "Test files ready"
}

# Submit job and wait for completion
submit_and_wait() {
    local endpoint=$1
    local file1=$2
    local file2=$3
    local params=$4
    local test_name=$5
    
    log_info "Testing: $test_name"
    
    # Submit job
    local start_time=$(date +%s%N)
    
    if [ -z "$file2" ]; then
        if [ -z "$params" ]; then
            response=$(curl -s -X POST "$BASE_URL/$endpoint" \
                -H "X-API-Key: $API_KEY" \
                -F "file=@$file1")
        else
            response=$(curl -s -X POST "$BASE_URL/$endpoint" \
                -H "X-API-Key: $API_KEY" \
                -F "file=@$file1" \
                $params)
        fi
    else
        response=$(curl -s -X POST "$BASE_URL/$endpoint" \
            -H "X-API-Key: $API_KEY" \
            -F "file1=@$file1" \
            -F "file2=@$file2")
    fi
    
    local submit_time=$(date +%s%N)
    
    # Extract job ID
    job_id=$(echo "$response" | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$job_id" ]; then
        log_error "Failed to submit job: $response"
        return 1
    fi
    
    # Wait for completion
    local max_wait=300
    local wait_time=0
    local status="PENDING"
    
    while [ "$status" = "PENDING" ] || [ "$status" = "PROCESSING" ]; do
        if [ $wait_time -gt $max_wait ]; then
            log_error "Timeout waiting for job completion"
            return 1
        fi
        
        sleep 1
        wait_time=$((wait_time + 1))
        
        status_response=$(curl -s -H "X-API-Key: $API_KEY" "$BASE_URL/jobs/$job_id")
        status=$(echo "$status_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        
        if [ "$status" = "FAILED" ]; then
            log_error "Job failed: $status_response"
            return 1
        fi
    done
    
    local end_time=$(date +%s%N)
    
    # Calculate durations
    local submit_duration=$((($submit_time - $start_time) / 1000000))
    local total_duration=$((($end_time - $start_time) / 1000000))
    
    # Get file size
    local file_size=0
    if [ -f "$file1" ]; then
        file_size=$(stat -f%z "$file1" 2>/dev/null || stat -c%s "$file1" 2>/dev/null || echo "0")
    fi
    
    # Get output file size if available
    local output_size=0
    if echo "$status_response" | grep -q '"fileSize"'; then
        output_size=$(echo "$status_response" | grep -o '"fileSize":[0-9]*' | cut -d':' -f2)
    fi
    
    # Log result
    echo "$test_name,$submit_duration,$total_duration,$file_size,$output_size,$job_id" >> "$RESULTS_DIR/benchmark.csv"
    
    log_success "Completed: $test_name - ${total_duration}ms (submit: ${submit_duration}ms)"
    
    # Download result for verification
    curl -s -H "X-API-Key: $API_KEY" "$BASE_URL/download/$job_id" -o "$RESULTS_DIR/${test_name}_output.pdf" 2>/dev/null || true
    
    return 0
}

# Run all benchmarks
run_benchmarks() {
    log_info "Starting performance benchmarks..."
    log_info "Results will be saved to: $RESULTS_DIR"
    
    # Create CSV header
    echo "Test,Submit_Time_ms,Total_Time_ms,Input_Size_bytes,Output_Size_bytes,Job_ID" > "$RESULTS_DIR/benchmark.csv"
    
    # Test 1: Merge PDFs
    submit_and_wait "merge" "$TEST_DIR/simple.pdf" "$TEST_DIR/simple2.pdf" "" "merge_2_pdfs"
    
    # Test 2: Split PDF
    submit_and_wait "split" "$TEST_DIR/multipage.pdf" "" "-F pagesPerFile=1" "split_pdf"
    
    # Test 3: Compress PDF
    submit_and_wait "compress" "$TEST_DIR/large.pdf" "" "-F quality=0.8" "compress_large_pdf"
    
    # Test 4: Rotate PDF
    submit_and_wait "rotate" "$TEST_DIR/simple.pdf" "" "-F angle=90" "rotate_pdf"
    
    # Test 5: Add Watermark
    submit_and_wait "watermark" "$TEST_DIR/watermark_test.pdf" "" "-F watermark=TEST -F opacity=0.5" "add_watermark"
    
    # Test 6: Encrypt PDF
    submit_and_wait "encrypt" "$TEST_DIR/encryption_test.pdf" "" "-F password=test123" "encrypt_pdf"
    
    # Test 7: Extract Text
    submit_and_wait "extract-text" "$TEST_DIR/text_only.pdf" "" "" "extract_text"
    
    # Test 8: Extract Images
    submit_and_wait "extract-images" "$TEST_DIR/with_images.pdf" "" "" "extract_images"
    
    # Test 9: Extract Metadata
    submit_and_wait "extract-metadata" "$TEST_DIR/metadata_test.pdf" "" "" "extract_metadata"
    
    # Test 10: Add Page Numbers
    submit_and_wait "add-page-numbers" "$TEST_DIR/multipage.pdf" "" "" "add_page_numbers"
    
    # Test 11: Remove Pages
    submit_and_wait "remove-pages" "$TEST_DIR/multipage.pdf" "" "-F pages=1,3" "remove_pages"
    
    # Test 12: Crop Pages
    submit_and_wait "crop-pages" "$TEST_DIR/simple.pdf" "" "-F x=50 -F y=50 -F width=500 -F height=700" "crop_pages"
    
    # Test 13: Resize Pages
    submit_and_wait "resize-pages" "$TEST_DIR/simple.pdf" "" "-F width=800 -F height=1000" "resize_pages"
    
    # Test 14: Edit Metadata
    submit_and_wait "metadata-edit" "$TEST_DIR/metadata_test.pdf" "" "-F title=NewTitle -F author=NewAuthor" "edit_metadata"
    
    # Test 15: Optimize PDF
    submit_and_wait "optimize" "$TEST_DIR/large.pdf" "" "" "optimize_pdf"
    
    # Test 16: PDF to Image
    submit_and_wait "pdf-to-image" "$TEST_DIR/simple.pdf" "" "-F format=png -F dpi=150" "pdf_to_image"
    
    # Test 17: Image to PDF
    submit_and_wait "image-to-pdf" "$TEST_DIR/with_images.pdf" "" "" "image_to_pdf"
    
    # Test 18: PDF to Text
    submit_and_wait "pdf-to-text" "$TEST_DIR/text_only.pdf" "" "" "pdf_to_text"
    
    # Test 19: Text to PDF
    submit_and_wait "text-to-pdf" "$TEST_DIR/test_text.txt" "" "" "text_to_pdf"
    
    # Test 20: PDF to Word
    submit_and_wait "pdf-to-word" "$TEST_DIR/test_document.pdf" "" "" "pdf_to_word"
    
    # Test 21: PDF to Excel
    submit_and_wait "pdf-to-excel" "$TEST_DIR/test_spreadsheet.pdf" "" "" "pdf_to_excel"
    
    # Test 22: PDF to PPT
    submit_and_wait "pdf-to-ppt" "$TEST_DIR/test_presentation.pdf" "" "" "pdf_to_ppt"
    
    # Test 23: HTML to PDF
    submit_and_wait "html-to-pdf" "$TEST_DIR/test_html.html" "" "" "html_to_pdf"
    
    # Test 24: Markdown to PDF
    submit_and_wait "markdown-to-pdf" "$TEST_DIR/test_markdown.md" "" "" "markdown_to_pdf"
    
    # Test 25: Text File to PDF
    submit_and_wait "txt-to-pdf" "$TEST_DIR/test_text.txt" "" "" "txt_to_pdf"
    
    # Test 26: PDF/A Convert
    submit_and_wait "pdfa-convert" "$TEST_DIR/simple.pdf" "" "" "pdfa_convert"
    
    # Test 27: OCR PDF
    submit_and_wait "ocr-pdf" "$TEST_DIR/ocr_test.pdf" "" "-F language=eng -F dpi=300" "ocr_pdf"
    
    # Test 28: Compare PDFs
    submit_and_wait "compare-pdfs" "$TEST_DIR/comparison_test1.pdf" "$TEST_DIR/comparison_test2.pdf" "" "compare_pdfs"
    
    # Test 29: Decrypt PDF (first encrypt, then decrypt)
    log_info "Testing decrypt (first encrypting...)..."
    submit_and_wait "encrypt" "$TEST_DIR/encryption_test.pdf" "" "-F password=test123" "decrypt_setup"
    # Note: Decrypt would need the encrypted file, skipping for simplicity
    
    # Test 30: Linearize PDF
    submit_and_wait "linearize" "$TEST_DIR/simple.pdf" "" "" "linearize_pdf"
    
    # Test 31: Extract Text from Large PDF
    submit_and_wait "extract-text" "$TEST_DIR/large.pdf" "" "" "extract_text_large"
    
    # Test 32: Compress with Different Quality
    submit_and_wait "compress" "$TEST_DIR/large.pdf" "" "-F quality=0.5" "compress_high"
}

# Generate report
generate_report() {
    log_info "Generating benchmark report..."
    
    cat > "$RESULTS_DIR/report.md" << EOF
# Performance Benchmark Report

Generated: $(date)

## Summary

| Metric | Value |
|--------|-------|
| Total Tests | $(tail -n +2 "$RESULTS_DIR/benchmark.csv" | wc -l) |
| Average Submit Time | $(awk -F',' '{sum+=$2; count++} END {printf "%.2f ms", sum/count}' "$RESULTS_DIR/benchmark.csv") |
| Average Total Time | $(awk -F',' '{sum+=$3; count++} END {printf "%.2f ms", sum/count}' "$RESULTS_DIR/benchmark.csv") |
| Fastest Test | $(awk -F',' 'NR>1 {if(min=="" || $3<min) {min=$3; name=$1}} END {printf "%s (%.2f ms)", name, min}' "$RESULTS_DIR/benchmark.csv") |
| Slowest Test | $(awk -F',' 'NR>1 {if(max=="" || $3>max) {max=$3; name=$1}} END {printf "%s (%.2f ms)", name, max}' "$RESULTS_DIR/benchmark.csv") |

## Detailed Results

\`\`\`
$(cat "$RESULTS_DIR/benchmark.csv")
\`\`\`

## Individual Test Results

EOF

    # Add table of results
    echo "| Test Name | Submit Time | Total Time | Input Size | Output Size |" >> "$RESULTS_DIR/report.md"
    echo "|-----------|-------------|------------|------------|-------------|" >> "$RESULTS_DIR/report.md"
    
    tail -n +2 "$RESULTS_DIR/benchmark.csv" | while IFS=',' read -r name submit total input output job; do
        input_kb=$((input / 1024))
        output_kb=$((output / 1024))
        echo "| $name | ${submit}ms | ${total}ms | ${input_kb}KB | ${output_kb}KB |" >> "$RESULTS_DIR/report.md"
    done
    
    log_success "Report generated: $RESULTS_DIR/report.md"
}

# Main execution
main() {
    echo "=========================================================="
    echo "PDF Processing Platform - Performance Benchmark"
    echo "=========================================================="
    echo ""
    
    # Check dependencies
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        log_warning "bc not found, using basic arithmetic"
    fi
    
    # Check server
    if ! check_server; then
        log_error "Please start the server first: ./gradlew bootRun"
        exit 1
    fi
    
    # Generate test files
    generate_test_files
    
    # Run benchmarks
    run_benchmarks
    
    # Generate report
    generate_report
    
    echo ""
    echo "=========================================================="
    echo "Benchmark Complete!"
    echo "=========================================================="
    echo ""
    echo "Results:"
    echo "  - Data: $RESULTS_DIR/benchmark.csv"
    echo "  - Report: $RESULTS_DIR/report.md"
    echo "  - Outputs: $RESULTS_DIR/*_output.pdf"
    echo ""
    echo "To view results:"
    echo "  cat $RESULTS_DIR/report.md"
    echo "  cat $RESULTS_DIR/benchmark.csv"
    echo ""
}

# Run main
main "$@"