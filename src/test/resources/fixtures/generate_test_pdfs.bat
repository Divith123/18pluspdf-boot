@echo off
REM Test PDF Generator for Windows
REM Generates sample PDF files for testing all 32 tools

echo ============================================================
echo PDF Processing Platform - Test File Generator
echo ============================================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python is not installed or not in PATH
    echo Please install Python 3.x and reportlab
    echo.
    echo To install reportlab:
    echo   pip install reportlab
    echo.
    pause
    exit /b 1
)

REM Check if reportlab is installed
python -c "import reportlab" >nul 2>&1
if errorlevel 1 (
    echo Error: reportlab library is not installed
    echo.
    echo Installing reportlab...
    pip install reportlab
    echo.
)

REM Create test_files directory
if not exist "test_files" mkdir test_files
cd test_files

echo Generating test files...
echo.

REM Run the Python script
python ..\generate_test_pdfs.py

echo.
echo ============================================================
echo Test files generated successfully!
echo ============================================================
echo.
echo Location: %CD%
echo.

dir /b /o

echo.
echo Total files:
dir /b /o | find /c /v ""
echo.

pause