package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for AI analysis operations.
 */
public class AIAnalysisResponse {
    
    private String jobId;
    private String analysisType;
    private AnalysisResult result;
    private double processingTimeMs;
    private String modelUsed;
    private LocalDateTime analyzedAt;
    private Map<String, Object> metadata;
    private String error;
    
    public static class AnalysisResult {
        // For summarization
        private String summary;
        private List<String> keyPoints;
        private List<String> keywords;
        
        // For search
        private List<SearchResult> searchResults;
        
        // For Q&A
        private List<QAResult> qaResults;
        
        // For NER
        private List<Entity> entities;
        
        // For categorization
        private List<Category> categories;
        
        // For table extraction
        private List<ExtractedTable> tables;
        
        // General confidence
        private double overallConfidence;
        
        // Getters and Setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }
        
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        
        public List<SearchResult> getSearchResults() { return searchResults; }
        public void setSearchResults(List<SearchResult> searchResults) { this.searchResults = searchResults; }
        
        public List<QAResult> getQaResults() { return qaResults; }
        public void setQaResults(List<QAResult> qaResults) { this.qaResults = qaResults; }
        
        public List<Entity> getEntities() { return entities; }
        public void setEntities(List<Entity> entities) { this.entities = entities; }
        
        public List<Category> getCategories() { return categories; }
        public void setCategories(List<Category> categories) { this.categories = categories; }
        
        public List<ExtractedTable> getTables() { return tables; }
        public void setTables(List<ExtractedTable> tables) { this.tables = tables; }
        
        public double getOverallConfidence() { return overallConfidence; }
        public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }
    }
    
    public static class SearchResult {
        private String text;
        private int pageNumber;
        private double score;
        private String context;
        private int startOffset;
        private int endOffset;
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        
        public int getStartOffset() { return startOffset; }
        public void setStartOffset(int startOffset) { this.startOffset = startOffset; }
        
        public int getEndOffset() { return endOffset; }
        public void setEndOffset(int endOffset) { this.endOffset = endOffset; }
    }
    
    public static class QAResult {
        private String question;
        private String answer;
        private double confidence;
        private List<String> sourcePages;
        private String context;
        
        // Getters and Setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public List<String> getSourcePages() { return sourcePages; }
        public void setSourcePages(List<String> sourcePages) { this.sourcePages = sourcePages; }
        
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
    
    public static class Entity {
        private String text;
        private String type; // PERSON, ORG, LOCATION, DATE, MONEY, etc.
        private int pageNumber;
        private double confidence;
        private int startOffset;
        private int endOffset;
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public int getStartOffset() { return startOffset; }
        public void setStartOffset(int startOffset) { this.startOffset = startOffset; }
        
        public int getEndOffset() { return endOffset; }
        public void setEndOffset(int endOffset) { this.endOffset = endOffset; }
    }
    
    public static class Category {
        private String name;
        private double confidence;
        private String description;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class ExtractedTable {
        private int pageNumber;
        private int tableIndex;
        private int rowCount;
        private int columnCount;
        private List<List<String>> data;
        private List<String> headers;
        private String csvUrl;
        private String excelUrl;
        
        // Getters and Setters
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        
        public int getTableIndex() { return tableIndex; }
        public void setTableIndex(int tableIndex) { this.tableIndex = tableIndex; }
        
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        
        public int getColumnCount() { return columnCount; }
        public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
        
        public List<List<String>> getData() { return data; }
        public void setData(List<List<String>> data) { this.data = data; }
        
        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        
        public String getCsvUrl() { return csvUrl; }
        public void setCsvUrl(String csvUrl) { this.csvUrl = csvUrl; }
        
        public String getExcelUrl() { return excelUrl; }
        public void setExcelUrl(String excelUrl) { this.excelUrl = excelUrl; }
    }
    
    // Main class Getters and Setters
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public AnalysisResult getResult() { return result; }
    public void setResult(AnalysisResult result) { this.result = result; }
    
    public double getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(double processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
