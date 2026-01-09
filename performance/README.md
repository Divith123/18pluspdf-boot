# Performance Testing

This directory contains performance testing and benchmarking tools for the PDF Processing Platform.

## Contents

- `benchmark.sh` - Linux/macOS benchmark script (tests all 32 tools)
- `benchmark.bat` - Windows benchmark script
- `load_test.js` - Node.js load testing script
- `package.json` - Node.js dependencies
- `README.md` - This file

## Quick Start

### 1. Generate Test Files
```bash
cd src/test/resources/fixtures
python generate_test_pdfs.py
cd ../../../performance
```

### 2. Run Benchmarks

#### Linux/macOS
```bash
chmod +x benchmark.sh
./benchmark.sh
```

#### Windows
```cmd
benchmark.bat
```

#### Node.js Load Test
```bash
npm install
npm run benchmark  # 10 users, 60 seconds
```

## Benchmark Scripts

### benchmark.sh / benchmark.bat

**What it tests:**
- All 32 PDF tools
- Various file sizes
- Submit time and total processing time
- Input/output file sizes

**Output:**
- `results/benchmark.csv` - Raw data
- `results/report.md` - Summary report
- `results/*_output.pdf` - Generated files

**Usage:**
```bash
# Basic benchmark (all 32 tools)
./benchmark.sh

# Custom parameters (edit script to modify)
```

### load_test.js

**What it tests:**
- Concurrent user simulation
- Random tool selection
- Request throughput
- Error rates

**Usage:**
```bash
# Install dependencies
npm install

# Run with defaults (10 users, 60 seconds)
npm run benchmark

# Custom parameters
node load_test.js <concurrent_users> <duration_seconds>

# Examples:
node load_test.js 5 30      # 5 users, 30 seconds
node load_test.js 20 120    # 20 users, 2 minutes (stress test)
```

**Output:**
- Console logs with real-time progress
- `results/load_test_report.md` - Summary
- `results/load_test_results.csv` - Detailed results

## Test Scenarios

The load test randomly selects from these 8 scenarios:

1. **Merge** - Merge 2 PDFs
2. **Split** - Split multi-page PDF
3. **Compress** - Compress large PDF
4. **Rotate** - Rotate pages
5. **Extract Text** - Text extraction
6. **PDF to Image** - Conversion
7. **HTML to PDF** - HTML conversion
8. **OCR** - OCR processing

## Performance Metrics

### Key Metrics Tracked

| Metric | Description | Target |
|--------|-------------|--------|
| **Throughput** | Requests per second | > 2 req/s |
| **Latency** | Average response time | < 10s |
| **Success Rate** | % of successful requests | > 95% |
| **Concurrency** | Simultaneous users | 10-50 |

### Expected Results (4-core, 8GB RAM)

| Operation | Time (ms) | Input Size | Output Size |
|-----------|-----------|------------|-------------|
| Merge 2 PDFs | 500-2000 | 10MB total | 10MB |
| Split PDF | 300-1500 | 25MB | 25MB (multiple) |
| Compress | 5000-15000 | 50MB | 15MB |
| OCR | 20000-60000 | 20MB | 25MB |
| HTML→PDF | 2000-5000 | 1MB HTML | 2MB PDF |
| Word→PDF | 2000-5000 | 5MB DOCX | 2MB PDF |

## Interpreting Results

### benchmark.sh Output

```csv
Test,Submit_Time_ms,Total_Time_ms,Input_Size_bytes,Output_Size_bytes,Job_ID
merge_2_pdfs,50,1200,10485760,10485760,job-123...
```

**Columns:**
- **Submit_Time_ms** - Time to submit job (network + validation)
- **Total_Time_ms** - End-to-end processing time
- **Input_Size_bytes** - Input file size
- **Output_Size_bytes** - Output file size

### load_test.js Output

**Summary:**
- **Requests/Second** - System throughput
- **Success Rate** - Reliability
- **Avg Duration** - Typical response time
- **User Requests** - Per-user throughput

## Optimization Tips

### If Throughput is Low

1. **Increase thread pool:**
```yaml
app:
  job-queue:
    max-pool-size: 128
    queue-capacity: 2000
```

2. **Use faster storage:**
   - SSD for temp/output directories
   - tmpfs for temporary files

3. **Scale horizontally:**
   - Run multiple instances
   - Use load balancer

### If Latency is High

1. **Check external tools:**
   - LibreOffice path
   - Tesseract configuration
   - MuPDF installation

2. **Optimize file sizes:**
   - Compress inputs
   - Reduce image resolutions

3. **Increase memory:**
```bash
java -Xmx8g -jar app.jar
```

### If Success Rate is Low

1. **Check logs:**
```bash
tail -f logs/error.log
```

2. **Verify dependencies:**
```bash
# LibreOffice
soffice --version

# Tesseract
tesseract --version

# MuPDF
mutool --version
```

3. **Check disk space:**
```bash
df -h
```

## Continuous Benchmarking

### Add to CI/CD

```yaml
# .github/workflows/benchmark.yml
name: Performance Benchmark

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Install Dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y libreoffice tesseract-ocr mupdf-tools
      - name: Start Server
        run: ./gradlew bootRun &
      - name: Wait for Server
        run: sleep 30
      - name: Generate Test Files
        run: |
          cd src/test/resources/fixtures
          python generate_test_pdfs.py
      - name: Run Benchmark
        run: ./performance/benchmark.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: performance/results/
```

## Troubleshooting

### Server Not Responding
```bash
# Check if server is running
curl http://localhost:8080/actuator/health

# Check logs
tail -f logs/application.log
```

### Missing Test Files
```bash
# Regenerate
cd src/test/resources/fixtures
python generate_test_pdfs.py
```

### External Tools Not Found
```bash
# Check paths
echo $APP_EXTERNAL_TOOLS_LIBREOFFICE_PATH
echo $APP_EXTERNAL_TOOLS_TESSERACT_PATH

# Test tools
$APP_EXTERNAL_TOOLS_LIBREOFFICE_PATH --version
$APP_EXTERNAL_TOOLS_TESSERACT_PATH --version
```

### High Memory Usage
```bash
# Monitor memory
htop

# Check Java heap
jstat -gc <pid> 1s

# Increase heap
export JAVA_OPTS="-Xmx8g -Xms2g"
```

## Advanced Testing

### Stress Testing
```bash
# High concurrency
node load_test.js 50 120

# Long duration
node load_test.js 10 600
```

### Specific Tool Testing
```bash
# Modify load_test.js to test only specific tools
const TESTS = [
    { name: 'merge', endpoint: 'merge', files: ['simple.pdf', 'simple2.pdf'], params: {} },
    { name: 'ocr', endpoint: 'ocr-pdf', files: ['ocr_test.pdf'], params: { language: 'eng', dpi: 300 } }
];
```

### Comparative Testing
```bash
# Run benchmark before and after changes
./benchmark.sh
# ... make changes ...
./benchmark.sh
# Compare results
diff results/benchmark.csv results/benchmark_new.csv
```

## Results Analysis

### Using CSV Data
```bash
# Average duration by test
awk -F',' 'NR>1 {sum[$1]+=$3; count[$1]++} END {for(t in sum) print t, sum[t]/count[t]}' results/benchmark.csv

# Fastest/slowest tests
sort -t',' -k3 -n results/benchmark.csv | head -10
sort -t',' -k3 -nr results/benchmark.csv | head -10
```

### Using Node.js Results
```bash
# Analyze CSV
awk -F',' 'NR>1 {sum+=$4; count++} END {print "Avg Duration:", sum/count}' results/load_test_results.csv
```

## Performance Targets

### Production Targets

| Metric | Development | Production |
|--------|-------------|------------|
| **Concurrent Users** | 5-10 | 50-100 |
| **Requests/Second** | 1-2 | 10-20 |
| **Avg Latency** | < 15s | < 10s |
| **Success Rate** | > 90% | > 99% |
| **Memory Usage** | < 2GB | < 4GB |

### Scaling Guidelines

**Vertical Scaling:**
- 4 cores, 8GB RAM: ~10 concurrent users
- 8 cores, 16GB RAM: ~25 concurrent users
- 16 cores, 32GB RAM: ~50 concurrent users

**Horizontal Scaling:**
- 2 instances: ~20 concurrent users
- 4 instances: ~40 concurrent users
- 8 instances: ~80 concurrent users

## Monitoring

### Real-time Monitoring
```bash
# Terminal 1: Watch logs
tail -f logs/application.log

# Terminal 2: Watch resource usage
htop

# Terminal 3: Watch network
watch -n 1 'netstat -an | grep :8080 | wc -l'
```

### Application Metrics
```bash
# Health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Conclusion

Regular performance testing ensures:
- System reliability under load
- Early detection of bottlenecks
- Capacity planning data
- Performance regression detection

Run benchmarks after:
- Code changes
- Configuration updates
- Infrastructure changes
- Dependency updates

**Happy testing!** 🚀