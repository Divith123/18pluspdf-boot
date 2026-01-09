package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for AI-powered document analysis.
 * Supports semantic search, Q&A, summarization, NER, and categorization.
 */
public class AIAnalysisRequest {
    
    @NotNull(message = "Analysis type is required")
    private AnalysisType analysisType;
    
    private String query; // For search, Q&A
    
    private List<String> questions; // For batch Q&A
    
    private List<String> entityTypes; // For NER filtering
    
    private SummaryOptions summaryOptions;
    
    private SearchOptions searchOptions;
    
    private CategoryOptions categoryOptions;
    
    private String language = "en";
    
    private boolean includeConfidenceScores = true;
    
    private boolean extractTables = true;
    
    private String modelProvider = "default"; // default, openai, azure, local
    
    private Map<String, Object> modelConfig;
    
    public enum AnalysisType {
        SEMANTIC_SEARCH,      // Find relevant passages
        DOCUMENT_QA,          // Answer questions about document
        SUMMARIZE,            // Generate summary
        EXTRACT_ENTITIES,     // NER: people, places, orgs, amounts
        CATEGORIZE,           // Auto-categorize document type
        EXTRACT_KEY_POINTS,   // Extract main takeaways
        SENTIMENT_ANALYSIS,   // Determine document sentiment
        COMPARE_DOCUMENTS,    // Semantic comparison of multiple docs
        TRANSLATE,            // Translate document content
        GENERATE_INDEX,       // Create searchable index
        EXTRACT_TABLES        // Structured table extraction
    }
    
    public static class SummaryOptions {
        private int maxLength = 500; // Max words
        private SummaryStyle style = SummaryStyle.PARAGRAPH;
        private boolean includeKeywords = true;
        private int topKeywordsCount = 10;
        
        public enum SummaryStyle {
            PARAGRAPH,
            BULLET_POINTS,
            EXECUTIVE_SUMMARY,
            ONE_LINE
        }
        
        // Getters and Setters
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        
        public SummaryStyle getStyle() { return style; }
        public void setStyle(SummaryStyle style) { this.style = style; }
        
        public boolean isIncludeKeywords() { return includeKeywords; }
        public void setIncludeKeywords(boolean includeKeywords) { this.includeKeywords = includeKeywords; }
        
        public int getTopKeywordsCount() { return topKeywordsCount; }
        public void setTopKeywordsCount(int topKeywordsCount) { this.topKeywordsCount = topKeywordsCount; }
    }
    
    public static class SearchOptions {
        private int topK = 10; // Number of results
        private float minScore = 0.5f; // Minimum relevance score
        private boolean highlightMatches = true;
        private int contextWindow = 100; // Characters around match
        private SearchMode mode = SearchMode.HYBRID;
        
        public enum SearchMode {
            KEYWORD,     // Traditional keyword search
            SEMANTIC,    // Vector similarity search
            HYBRID       // Combined approach
        }
        
        // Getters and Setters
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        
        public float getMinScore() { return minScore; }
        public void setMinScore(float minScore) { this.minScore = minScore; }
        
        public boolean isHighlightMatches() { return highlightMatches; }
        public void setHighlightMatches(boolean highlightMatches) { this.highlightMatches = highlightMatches; }
        
        public int getContextWindow() { return contextWindow; }
        public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }
        
        public SearchMode getMode() { return mode; }
        public void setMode(SearchMode mode) { this.mode = mode; }
    }
    
    public static class CategoryOptions {
        private List<String> customCategories; // If null, use default detection
        private boolean multiLabel = true; // Allow multiple categories
        private float threshold = 0.7f; // Category confidence threshold
        
        // Getters and Setters
        public List<String> getCustomCategories() { return customCategories; }
        public void setCustomCategories(List<String> customCategories) { this.customCategories = customCategories; }
        
        public boolean isMultiLabel() { return multiLabel; }
        public void setMultiLabel(boolean multiLabel) { this.multiLabel = multiLabel; }
        
        public float getThreshold() { return threshold; }
        public void setThreshold(float threshold) { this.threshold = threshold; }
    }
    
    // Main class Getters and Setters
    
    public AnalysisType getAnalysisType() {
        return analysisType;
    }
    
    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public List<String> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
    
    public List<String> getEntityTypes() {
        return entityTypes;
    }
    
    public void setEntityTypes(List<String> entityTypes) {
        this.entityTypes = entityTypes;
    }
    
    public SummaryOptions getSummaryOptions() {
        return summaryOptions;
    }
    
    public void setSummaryOptions(SummaryOptions summaryOptions) {
        this.summaryOptions = summaryOptions;
    }
    
    public SearchOptions getSearchOptions() {
        return searchOptions;
    }
    
    public void setSearchOptions(SearchOptions searchOptions) {
        this.searchOptions = searchOptions;
    }
    
    public CategoryOptions getCategoryOptions() {
        return categoryOptions;
    }
    
    public void setCategoryOptions(CategoryOptions categoryOptions) {
        this.categoryOptions = categoryOptions;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public boolean isIncludeConfidenceScores() {
        return includeConfidenceScores;
    }
    
    public void setIncludeConfidenceScores(boolean includeConfidenceScores) {
        this.includeConfidenceScores = includeConfidenceScores;
    }
    
    public boolean isExtractTables() {
        return extractTables;
    }
    
    public void setExtractTables(boolean extractTables) {
        this.extractTables = extractTables;
    }
    
    public String getModelProvider() {
        return modelProvider;
    }
    
    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }
    
    public Map<String, Object> getModelConfig() {
        return modelConfig;
    }
    
    public void setModelConfig(Map<String, Object> modelConfig) {
        this.modelConfig = modelConfig;
    }
}
