/**
 * Load Testing Script for PDF Processing Platform
 * Uses Node.js with axios for concurrent requests
 * 
 * Usage: node load_test.js [concurrent_users] [duration_seconds]
 */

const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');

// Configuration
const CONFIG = {
    baseUrl: 'http://localhost:8080/api/pdf',
    apiKey: 'admin-key-12345',
    concurrentUsers: parseInt(process.argv[2]) || 10,
    duration: parseInt(process.argv[3]) || 60,
    testDir: './test_files',
    resultsDir: './results',
    timeout: 300000 // 5 minutes
};

// Test scenarios
const TESTS = [
    { name: 'merge', endpoint: 'merge', files: ['simple.pdf', 'simple2.pdf'], params: {} },
    { name: 'split', endpoint: 'split', files: ['multipage.pdf'], params: { pagesPerFile: 1 } },
    { name: 'compress', endpoint: 'compress', files: ['large.pdf'], params: { quality: 0.8 } },
    { name: 'rotate', endpoint: 'rotate', files: ['simple.pdf'], params: { angle: 90 } },
    { name: 'extract-text', endpoint: 'extract-text', files: ['text_only.pdf'], params: {} },
    { name: 'pdf-to-image', endpoint: 'pdf-to-image', files: ['simple.pdf'], params: { format: 'png', dpi: 150 } },
    { name: 'html-to-pdf', endpoint: 'html-to-pdf', files: ['test_html.html'], params: {} },
    { name: 'ocr-pdf', endpoint: 'ocr-pdf', files: ['ocr_test.pdf'], params: { language: 'eng', dpi: 300 } }
];

// Results storage
const results = {
    startTime: null,
    endTime: null,
    requests: [],
    summary: {}
};

// Helper functions
function log(message, level = 'info') {
    const timestamp = new Date().toISOString();
    const colors = {
        info: '\x1b[36m',
        success: '\x1b[32m',
        warning: '\x1b[33m',
        error: '\x1b[31m',
        reset: '\x1b[0m'
    };
    console.log(`${colors[level]}[${timestamp}] ${message}${colors.reset}`);
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function ensureDirectories() {
    if (!fs.existsSync(CONFIG.testDir)) {
        fs.mkdirSync(CONFIG.testDir, { recursive: true });
    }
    if (!fs.existsSync(CONFIG.resultsDir)) {
        fs.mkdirSync(CONFIG.resultsDir, { recursive: true });
    }
}

async function checkServer() {
    try {
        const response = await axios.get(`${CONFIG.baseUrl}/health`, {
            headers: { 'X-API-Key': CONFIG.apiKey },
            timeout: 5000
        });
        log('Server is running', 'success');
        return true;
    } catch (error) {
        log(`Server check failed: ${error.message}`, 'error');
        return false;
    }
}

async function submitJob(test, userIndex) {
    const startTime = Date.now();
    const formData = new FormData();
    
    // Add files
    for (const file of test.files) {
        const filePath = path.join(CONFIG.testDir, file);
        if (fs.existsSync(filePath)) {
            formData.append('file', fs.createReadStream(filePath));
        } else {
            log(`File not found: ${filePath}`, 'error');
            return null;
        }
    }
    
    // Add parameters
    for (const [key, value] of Object.entries(test.params)) {
        formData.append(key, value);
    }
    
    try {
        const response = await axios.post(
            `${CONFIG.baseUrl}/${test.endpoint}`,
            formData,
            {
                headers: {
                    ...formData.getHeaders(),
                    'X-API-Key': CONFIG.apiKey
                },
                timeout: CONFIG.timeout
            }
        );
        
        const endTime = Date.now();
        const duration = endTime - startTime;
        
        const result = {
            userIndex,
            test: test.name,
            startTime,
            endTime,
            duration,
            jobId: response.data.jobId,
            status: response.data.status,
            success: true
        };
        
        results.requests.push(result);
        log(`User ${userIndex}: ${test.name} completed in ${duration}ms`, 'success');
        
        return result;
        
    } catch (error) {
        const endTime = Date.now();
        const duration = endTime - startTime;
        
        const result = {
            userIndex,
            test: test.name,
            startTime,
            endTime,
            duration,
            error: error.message,
            success: false
        };
        
        results.requests.push(result);
        log(`User ${userIndex}: ${test.name} failed - ${error.message}`, 'error');
        
        return result;
    }
}

async function waitForJob(jobId, maxWait = 300000) {
    const startTime = Date.now();
    
    while (Date.now() - startTime < maxWait) {
        try {
            const response = await axios.get(`${CONFIG.baseUrl}/jobs/${jobId}`, {
                headers: { 'X-API-Key': CONFIG.apiKey },
                timeout: 10000
            });
            
            const status = response.data.status;
            
            if (status === 'COMPLETED') {
                return { success: true, data: response.data };
            } else if (status === 'FAILED') {
                return { success: false, error: 'Job failed', data: response.data };
            } else if (status === 'CANCELLED') {
                return { success: false, error: 'Job cancelled', data: response.data };
            }
            
            await sleep(1000);
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
    
    return { success: false, error: 'Timeout waiting for job completion' };
}

async function runLoadTest() {
    log(`Starting load test with ${CONFIG.concurrentUsers} concurrent users for ${CONFIG.duration} seconds`, 'info');
    
    results.startTime = Date.now();
    const endTime = results.startTime + (CONFIG.duration * 1000);
    
    const activeUsers = new Array(CONFIG.concurrentUsers).fill(null);
    const userPromises = [];
    
    // Start user simulation
    for (let i = 0; i < CONFIG.concurrentUsers; i++) {
        const userPromise = (async (userIndex) => {
            let requestCount = 0;
            
            while (Date.now() < endTime) {
                // Select random test
                const test = TESTS[Math.floor(Math.random() * TESTS.length)];
                
                // Submit job
                const result = await submitJob(test, userIndex);
                
                if (result && result.success && result.jobId) {
                    // Wait for completion
                    const waitResult = await waitForJob(result.jobId);
                    
                    if (waitResult.success) {
                        requestCount++;
                        log(`User ${userIndex}: Completed ${requestCount} requests`, 'info');
                    }
                }
                
                // Small delay between requests
                await sleep(100 + Math.random() * 200);
            }
            
            return { userIndex, requestCount };
        })(i);
        
        userPromises.push(userPromise);
    }
    
    // Wait for all users to complete
    const userResults = await Promise.all(userPromises);
    
    results.endTime = Date.now();
    
    // Calculate summary
    const totalTime = results.endTime - results.startTime;
    const totalRequests = results.requests.length;
    const successfulRequests = results.requests.filter(r => r.success).length;
    const failedRequests = results.requests.filter(r => !r.success).length;
    
    const durations = results.requests
        .filter(r => r.success)
        .map(r => r.duration);
    
    results.summary = {
        totalTime,
        totalRequests,
        successfulRequests,
        failedRequests,
        successRate: (successfulRequests / totalRequests * 100).toFixed(2),
        avgDuration: durations.length > 0 ? durations.reduce((a, b) => a + b, 0) / durations.length : 0,
        minDuration: durations.length > 0 ? Math.min(...durations) : 0,
        maxDuration: durations.length > 0 ? Math.max(...durations) : 0,
        requestsPerSecond: totalRequests / (totalTime / 1000),
        userResults
    };
}

function generateReport() {
    const reportPath = path.join(CONFIG.resultsDir, 'load_test_report.md');
    const csvPath = path.join(CONFIG.resultsDir, 'load_test_results.csv');
    
    // Generate CSV
    let csv = 'User,Test,Duration(ms),Success,JobID,Error\n';
    results.requests.forEach(r => {
        csv += `${r.userIndex},${r.test},${r.duration},${r.success},${r.jobId || ''},${r.error || ''}\n`;
    });
    fs.writeFileSync(csvPath, csv);
    
    // Generate Markdown report
    const report = `# Load Test Report

## Configuration
- **Concurrent Users**: ${CONFIG.concurrentUsers}
- **Duration**: ${CONFIG.duration} seconds
- **Test Scenarios**: ${TESTS.map(t => t.name).join(', ')}
- **Start Time**: ${new Date(results.startTime).toISOString()}
- **End Time**: ${new Date(results.endTime).toISOString()}

## Summary
- **Total Requests**: ${results.summary.totalRequests}
- **Successful**: ${results.summary.successfulRequests}
- **Failed**: ${results.summary.failedRequests}
- **Success Rate**: ${results.summary.successRate}%
- **Total Time**: ${(results.summary.totalTime / 1000).toFixed(2)}s
- **Requests/Second**: ${results.summary.requestsPerSecond.toFixed(2)}
- **Avg Duration**: ${results.summary.avgDuration.toFixed(2)}ms
- **Min Duration**: ${results.summary.minDuration.toFixed(2)}ms
- **Max Duration**: ${results.summary.maxDuration.toFixed(2)}ms

## User Results
| User | Requests |
|------|----------|
${results.summary.userResults.map(u => `| ${u.userIndex} | ${u.requestCount} |`).join('\n')}

## Detailed Results
See: ${csvPath}

## Recommendations
${results.summary.successRate < 95 ? '- ⚠️ Success rate is below 95%, investigate failures' : '- ✅ Success rate is good'}
${results.summary.avgDuration > 10000 ? '- ⚠️ Average duration is high, consider optimization' : '- ✅ Response times are acceptable'}
${results.summary.requestsPerSecond < 1 ? '- ⚠️ Low throughput, consider scaling' : '- ✅ Throughput is good'}
`;
    
    fs.writeFileSync(reportPath, report);
    
    log(`Report generated: ${reportPath}`, 'success');
    log(`CSV data: ${csvPath}`, 'success');
}

function printSummary() {
    console.log('\n' + '='.repeat(60));
    console.log('LOAD TEST SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total Requests:    ${results.summary.totalRequests}`);
    console.log(`Successful:        ${results.summary.successfulRequests}`);
    console.log(`Failed:            ${results.summary.failedRequests}`);
    console.log(`Success Rate:      ${results.summary.successRate}%`);
    console.log(`Total Time:        ${(results.summary.totalTime / 1000).toFixed(2)}s`);
    console.log(`Requests/Second:   ${results.summary.requestsPerSecond.toFixed(2)}`);
    console.log(`Avg Duration:      ${results.summary.avgDuration.toFixed(2)}ms`);
    console.log(`Min Duration:      ${results.summary.minDuration.toFixed(2)}ms`);
    console.log(`Max Duration:      ${results.summary.maxDuration.toFixed(2)}ms`);
    console.log('='.repeat(60));
}

async function main() {
    try {
        ensureDirectories();
        
        if (!await checkServer()) {
            log('Please start the server first: ./gradlew bootRun', 'error');
            process.exit(1);
        }
        
        await runLoadTest();
        generateReport();
        printSummary();
        
        log('Load test completed successfully!', 'success');
        
    } catch (error) {
        log(`Load test failed: ${error.message}`, 'error');
        console.error(error);
        process.exit(1);
    }
}

// Run if called directly
if (require.main === module) {
    main();
}

module.exports = { CONFIG, TESTS, submitJob, waitForJob, runLoadTest };