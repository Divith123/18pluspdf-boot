@echo off
REM Performance Benchmark Script for Windows
REM Tests all 32 tools with various file sizes

setlocal enabledelayedexpansion

REM Configuration
set BASE_URL=http://localhost:8080/api/pdf
set API_KEY=admin-key-12345
set TEST_DIR=.\test_files
set RESULTS_DIR=.\results

REM Create directories
if not exist "%RESULTS_DIR%" mkdir "%RESULTS_DIR%"
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"

REM Colors (using ANSI codes for PowerShell compatibility)
set RED=[91m
set GREEN=[92m
set YELLOW=[93m
set BLUE=[94m
set NC=[0m

REM Helper functions
:log_info
echo %BLUE%[INFO]%NC% %~1
goto :eof

:log_success
echo %GREEN%[SUCCESS]%NC% %~1
goto :eof

:log_warning
echo %YELLOW%[WARNING]%NC% %~1
goto :eof

:log_error
echo %RED%[ERROR]%NC% %~1
goto :eof

REM Check if server is running
:check_server
call :log_info "Checking if server is running..."
curl -s -H "X-API-Key: %API_KEY%" "%BASE_URL%/health" > nul 2>&1
if %errorlevel% equ 0 (
    call :log_success "Server is running"
    exit /b 0
) else (
    call :log_error "Server is not running at %BASE_URL%"
    exit /b 1
)

REM Submit job and wait for completion
:submit_and_wait
set endpoint=%~1
set file1=%~2
set file2=%~3
set params=%~4
set test_name=%~5

call :log_info "Testing: %test_name%"

REM Submit job
set start_time=!time!

if "!file2!"=="" (
    if "!params!"=="" (
        curl -s -X POST "%BASE_URL%/%endpoint%" ^
            -H "X-API-Key: %API_KEY%" ^
            -F "file=@%file1%" > "%RESULTS_DIR%\response.json"
    ) else (
        REM Parse params (space-separated -F key=value)
        set "curl_params="
        for %%p in (!params!) do (
            set "curl_params=!curl_params! -F %%p"
        )
        curl -s -X POST "%BASE_URL%/%endpoint%" ^
            -H "X-API-Key: %API_KEY%" ^
            -F "file=@%file1%" !curl_params! > "%RESULTS_DIR%\response.json"
    )
) else (
    curl -s -X POST "%BASE_URL%/%endpoint%" ^
        -H "X-API-Key: %API_KEY%" ^
        -F "file1=@%file1%" ^
        -F "file2=@%file2%" > "%RESULTS_DIR%\response.json"
)

set end_time=!time!

REM Extract job ID from response
for /f "tokens=2 delims=:" %%a in ('type "%RESULTS_DIR%\response.json" ^| findstr "jobId"') do (
    set "job_id=%%a"
    set "job_id=!job_id:"=!"
    set "job_id=!job_id: =!"
    set "job_id=!job_id:,=!"
    set "job_id=!job_id:}=!"
)

if "!job_id!"=="" (
    call :log_error "Failed to submit job"
    type "%RESULTS_DIR%\response.json"
    exit /b 1
)

REM Wait for completion
set max_wait=300
set wait_time=0
set status=PENDING

:wait_loop
if !wait_time! gtr !max_wait! (
    call :log_error "Timeout waiting for job completion"
    exit /b 1
)

timeout /t 1 /nobreak > nul
set /a wait_time+=1

curl -s -H "X-API-Key: %API_KEY%" "%BASE_URL%/jobs/!job_id!" > "%RESULTS_DIR%\status.json"

for /f "tokens=2 delims=:" %%a in ('type "%RESULTS_DIR%\status.json" ^| findstr "status"') do (
    set "status=%%a"
    set "status=!status:"=!"
    set "status=!status: =!"
    set "status=!status:,=!"
    set "status=!status:}=!"
)

if "!status!"=="PENDING" goto wait_loop
if "!status!"=="PROCESSING" goto wait_loop

if "!status!"=="FAILED" (
    call :log_error "Job failed"
    type "%RESULTS_DIR%\status.json"
    exit /b 1
)

REM Calculate duration
set /a duration=0
REM Note: Windows batch doesn't have easy timestamp math, using simple counter

REM Get file size
for %%~a in "%file1%" do set file_size=%%~za
if "!file_size!"=="" set file_size=0

REM Get output size
set output_size=0
for /f "tokens=2 delims=:" %%a in ('type "%RESULTS_DIR%\status.json" ^| findstr "fileSize"') do (
    set "output_size=%%a"
    set "output_size=!output_size:"=!"
    set "output_size=!output_size: =!"
    set "output_size=!output_size:,=!"
    set "output_size=!output_size:}=!"
)
if "!output_size!"=="" set output_size=0

REM Log result
echo %test_name%,!duration!,!duration!,!file_size!,!output_size!,!job_id! >> "%RESULTS_DIR%\benchmark.csv"

call :log_success "Completed: %test_name% - !duration!ms"

REM Download result
curl -s -H "X-API-Key: %API_KEY%" "%BASE_URL%/download/!job_id!" -o "%RESULTS_DIR%\%test_name%_output.pdf" 2>nul

exit /b 0

REM Generate test files
:generate_test_files
call :log_info "Generating test files..."
cd src\test\resources\fixtures
python generate_test_pdfs.py
cd ..\..\..
xcopy /Y /S src\test\resources\fixtures\test_files\*.* test_files\ >nul 2>&1
call :log_success "Test files ready"
goto :eof

REM Run all benchmarks
:run_benchmarks
call :log_info "Starting performance benchmarks..."
echo Test,Submit_Time_ms,Total_Time_ms,Input_Size_bytes,Output_Size_bytes,Job_ID > "%RESULTS_DIR%\benchmark.csv"

REM Test 1: Merge PDFs
call :submit_and_wait "merge" "test_files\simple.pdf" "test_files\simple2.pdf" "" "merge_2_pdfs"

REM Test 2: Split PDF
call :submit_and_wait "split" "test_files\multipage.pdf" "" "pagesPerFile=1" "split_pdf"

REM Test 3: Compress PDF
call :submit_and_wait "compress" "test_files\large.pdf" "" "quality=0.8" "compress_large_pdf"

REM Test 4: Rotate PDF
call :submit_and_wait "rotate" "test_files\simple.pdf" "" "angle=90" "rotate_pdf"

REM Test 5: Add Watermark
call :submit_and_wait "watermark" "test_files\watermark_test.pdf" "" "watermark=TEST opacity=0.5" "add_watermark"

REM Test 6: Encrypt PDF
call :submit_and_wait "encrypt" "test_files\encryption_test.pdf" "" "password=test123" "encrypt_pdf"

REM Test 7: Extract Text
call :submit_and_wait "extract-text" "test_files\text_only.pdf" "" "" "extract_text"

REM Test 8: Extract Images
call :submit_and_wait "extract-images" "test_files\with_images.pdf" "" "" "extract_images"

REM Test 9: Extract Metadata
call :submit_and_wait "extract-metadata" "test_files\metadata_test.pdf" "" "" "extract_metadata"

REM Test 10: Add Page Numbers
call :submit_and_wait "add-page-numbers" "test_files\multipage.pdf" "" "" "add_page_numbers"

REM Test 11: Remove Pages
call :submit_and_wait "remove-pages" "test_files\multipage.pdf" "" "pages=1,3" "remove_pages"

REM Test 12: Crop Pages
call :submit_and_wait "crop-pages" "test_files\simple.pdf" "" "x=50 y=50 width=500 height=700" "crop_pages"

REM Test 13: Resize Pages
call :submit_and_wait "resize-pages" "test_files\simple.pdf" "" "width=800 height=1000" "resize_pages"

REM Test 14: Edit Metadata
call :submit_and_wait "metadata-edit" "test_files\metadata_test.pdf" "" "title=NewTitle author=NewAuthor" "edit_metadata"

REM Test 15: Optimize PDF
call :submit_and_wait "optimize" "test_files\large.pdf" "" "" "optimize_pdf"

REM Test 16: PDF to Image
call :submit_and_wait "pdf-to-image" "test_files\simple.pdf" "" "format=png dpi=150" "pdf_to_image"

REM Test 17: Image to PDF
call :submit_and_wait "image-to-pdf" "test_files\with_images.pdf" "" "" "image_to_pdf"

REM Test 18: PDF to Text
call :submit_and_wait "pdf-to-text" "test_files\text_only.pdf" "" "" "pdf_to_text"

REM Test 19: Text to PDF
call :submit_and_wait "text-to-pdf" "test_files\test_text.txt" "" "" "text_to_pdf"

REM Test 20: PDF to Word
call :submit_and_wait "pdf-to-word" "test_files\test_document.pdf" "" "" "pdf_to_word"

REM Test 21: PDF to Excel
call :submit_and_wait "pdf-to-excel" "test_files\test_spreadsheet.pdf" "" "" "pdf_to_excel"

REM Test 22: PDF to PPT
call :submit_and_wait "pdf-to-ppt" "test_files\test_presentation.pdf" "" "" "pdf_to_ppt"

REM Test 23: HTML to PDF
call :submit_and_wait "html-to-pdf" "test_files\test_html.html" "" "" "html_to_pdf"

REM Test 24: Markdown to PDF
call :submit_and_wait "markdown-to-pdf" "test_files\test_markdown.md" "" "" "markdown_to_pdf"

REM Test 25: Text File to PDF
call :submit_and_wait "txt-to-pdf" "test_files\test_text.txt" "" "" "txt_to_pdf"

REM Test 26: PDF/A Convert
call :submit_and_wait "pdfa-convert" "test_files\simple.pdf" "" "" "pdfa_convert"

REM Test 27: OCR PDF
call :submit_and_wait "ocr-pdf" "test_files\ocr_test.pdf" "" "language=eng dpi=300" "ocr_pdf"

REM Test 28: Compare PDFs
call :submit_and_wait "compare-pdfs" "test_files\comparison_test1.pdf" "test_files\comparison_test2.pdf" "" "compare_pdfs"

REM Test 29: Linearize PDF
call :submit_and_wait "linearize" "test_files\simple.pdf" "" "" "linearize_pdf"

REM Test 30: Extract Text from Large PDF
call :submit_and_wait "extract-text" "test_files\large.pdf" "" "" "extract_text_large"

REM Test 31: Compress with Different Quality
call :submit_and_wait "compress" "test_files\large.pdf" "" "quality=0.5" "compress_high"

REM Test 32: Decrypt PDF (setup)
call :submit_and_wait "encrypt" "test_files\encryption_test.pdf" "" "password=test123" "decrypt_setup"

goto :eof

REM Generate report
:generate_report
call :log_info "Generating benchmark report..."

(
echo # Performance Benchmark Report
echo.
echo Generated: %date% %time%
echo.
echo ## Summary
echo.
echo See benchmark.csv for detailed results
echo.
echo ## Results
echo.
type "%RESULTS_DIR%\benchmark.csv"
) > "%RESULTS_DIR%\report.md"

call :log_success "Report generated: %RESULTS_DIR%\report.md"
goto :eof

REM Main execution
:main
echo ============================================================
echo PDF Processing Platform - Performance Benchmark
echo ============================================================
echo.

REM Check dependencies
where curl >nul 2>&1
if %errorlevel% neq 0 (
    call :log_error "curl is required but not installed"
    exit /b 1
)

REM Check server
call :check_server
if %errorlevel% neq 0 (
    call :log_error "Please start the server first: gradlew bootRun"
    exit /b 1
)

REM Generate test files
call :generate_test_files

REM Run benchmarks
call :run_benchmarks

REM Generate report
call :generate_report

echo.
echo ============================================================
echo Benchmark Complete!
echo ============================================================
echo.
echo Results:
echo   - Data: %RESULTS_DIR%\benchmark.csv
echo   - Report: %RESULTS_DIR%\report.md
echo.
echo To view results:
echo   type %RESULTS_DIR%\benchmark.csv
echo.

exit /b 0

REM Start main
call :main
endlocal