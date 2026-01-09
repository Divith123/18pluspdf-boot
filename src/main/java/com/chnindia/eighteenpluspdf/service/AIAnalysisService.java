package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.request.AIAnalysisRequest;
import com.chnindia.eighteenpluspdf.dto.response.AIAnalysisResponse;
import com.chnindia.eighteenpluspdf.dto.response.AIAnalysisResponse.*;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for AI-powered document analysis.
 * Provides semantic search, summarization, entity extraction, Q&A, and categorization.
 * 
 * Note: This service provides local text analysis capabilities.
 * For advanced AI features (GPT-style Q&A, semantic embeddings), integrate with external AI providers.
 */
@Service
public class AIAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisService.class);
    
    // Named Entity patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{1,3}[-.]?\\(?[0-9]{3}\\)?[-.]?[0-9]{3}[-.]?[0-9]{4}");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY_PATTERN = Pattern.compile("[$€£¥]\\s*[0-9,]+\\.?[0-9]*|[0-9,]+\\.?[0-9]*\\s*(?:USD|EUR|GBP|INR|dollars?|euros?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}[/\\w.-]*");
    
    // Document type keywords for categorization
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put("INVOICE", Arrays.asList("invoice", "bill", "payment due", "amount due", "total due", "invoice number", "billing"));
        CATEGORY_KEYWORDS.put("CONTRACT", Arrays.asList("agreement", "contract", "hereby agree", "terms and conditions", "party", "whereas", "binding"));
        CATEGORY_KEYWORDS.put("RESUME", Arrays.asList("experience", "education", "skills", "career", "employment", "curriculum vitae", "cv", "objective"));
        CATEGORY_KEYWORDS.put("REPORT", Arrays.asList("executive summary", "findings", "analysis", "conclusion", "methodology", "results", "report"));
        CATEGORY_KEYWORDS.put("LETTER", Arrays.asList("dear", "sincerely", "regards", "to whom it may concern", "yours truly"));
        CATEGORY_KEYWORDS.put("FORM", Arrays.asList("please fill", "signature", "applicant", "date of birth", "name:", "address:"));
        CATEGORY_KEYWORDS.put("MANUAL", Arrays.asList("instructions", "step 1", "how to", "guide", "tutorial", "user manual"));
        CATEGORY_KEYWORDS.put("LEGAL", Arrays.asList("court", "plaintiff", "defendant", "jurisdiction", "statute", "legal", "law"));
        CATEGORY_KEYWORDS.put("FINANCIAL", Arrays.asList("balance sheet", "income statement", "assets", "liabilities", "revenue", "expenses"));
        CATEGORY_KEYWORDS.put("ACADEMIC", Arrays.asList("abstract", "introduction", "references", "bibliography", "thesis", "research"));
    }
    
    /**
     * Perform document analysis based on the requested type.
     */
    public AIAnalysisResponse analyzeDocument(Path inputFile, AIAnalysisRequest request) {
        logger.info("Analyzing document: {} with type: {}", inputFile, request.getAnalysisType());
        
        long startTime = System.currentTimeMillis();
        
        AIAnalysisResponse response = new AIAnalysisResponse();
        response.setAnalyzedAt(LocalDateTime.now());
        response.setAnalysisType(request.getAnalysisType().name());
        response.setModelUsed("local-text-analysis");
        
        AnalysisResult result = new AnalysisResult();
        
        try {
            // Extract text from PDF
            Map<Integer, String> pageTexts = extractTextByPage(inputFile);
            String fullText = pageTexts.values().stream().collect(Collectors.joining("\n\n"));
            
            switch (request.getAnalysisType()) {
                case SEMANTIC_SEARCH:
                    result.setSearchResults(performSearch(pageTexts, request.getQuery(), request.getSearchOptions()));
                    break;
                    
                case DOCUMENT_QA:
                    result.setQaResults(answerQuestions(fullText, request.getQuestions(), request.getQuery()));
                    break;
                    
                case SUMMARIZE:
                    performSummarization(fullText, result, request.getSummaryOptions());
                    break;
                    
                case EXTRACT_ENTITIES:
                    result.setEntities(extractEntities(pageTexts, request.getEntityTypes()));
                    break;
                    
                case CATEGORIZE:
                    result.setCategories(categorizeDocument(fullText, request.getCategoryOptions()));
                    break;
                    
                case EXTRACT_KEY_POINTS:
                    result.setKeyPoints(extractKeyPoints(fullText));
                    break;
                    
                case EXTRACT_TABLES:
                    result.setTables(extractTables(pageTexts));
                    break;
                    
                default:
                    throw new PDFProcessingException("UNSUPPORTED_ANALYSIS", 
                        "Analysis type not supported: " + request.getAnalysisType());
            }
            
            result.setOverallConfidence(0.85); // Base confidence for local analysis
            
        } catch (IOException e) {
            throw new PDFProcessingException("ANALYSIS_ERROR", "Failed to analyze document: " + e.getMessage());
        }
        
        response.setResult(result);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return response;
    }
    
    private Map<Integer, String> extractTextByPage(Path inputFile) throws IOException {
        Map<Integer, String> pageTexts = new LinkedHashMap<>();
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                pageTexts.put(i, text);
            }
        }
        
        return pageTexts;
    }
    
    /**
     * Perform keyword search with ranking.
     */
    private List<SearchResult> performSearch(Map<Integer, String> pageTexts, String query, 
                                              AIAnalysisRequest.SearchOptions options) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        int topK = options != null ? options.getTopK() : 10;
        int contextWindow = options != null ? options.getContextWindow() : 100;
        float minScore = options != null ? options.getMinScore() : 0.3f;
        
        List<SearchResult> results = new ArrayList<>();
        String[] queryTerms = query.toLowerCase().split("\\s+");
        
        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNumber = entry.getKey();
            String text = entry.getValue();
            String lowerText = text.toLowerCase();
            
            // Find all occurrences
            for (String term : queryTerms) {
                int index = 0;
                while ((index = lowerText.indexOf(term, index)) != -1) {
                    SearchResult result = new SearchResult();
                    result.setPageNumber(pageNumber);
                    result.setStartOffset(index);
                    result.setEndOffset(index + term.length());
                    
                    // Extract context
                    int contextStart = Math.max(0, index - contextWindow);
                    int contextEnd = Math.min(text.length(), index + term.length() + contextWindow);
                    result.setContext(text.substring(contextStart, contextEnd).trim());
                    result.setText(term);
                    
                    // Calculate score based on term frequency and position
                    double score = calculateSearchScore(lowerText, term, index);
                    result.setScore(score);
                    
                    if (score >= minScore) {
                        results.add(result);
                    }
                    
                    index += term.length();
                }
            }
        }
        
        // Sort by score and limit
        return results.stream()
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    private double calculateSearchScore(String text, String term, int position) {
        // Simple TF-IDF-like scoring
        int termCount = countOccurrences(text, term);
        double tf = Math.log(1 + termCount);
        
        // Bonus for early occurrence
        double positionBonus = 1.0 - (position / (double) text.length()) * 0.5;
        
        return Math.min(1.0, tf * positionBonus * 0.5);
    }
    
    private int countOccurrences(String text, String term) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }
    
    /**
     * Answer questions about the document.
     */
    private List<QAResult> answerQuestions(String fullText, List<String> questions, String singleQuery) {
        List<QAResult> results = new ArrayList<>();
        
        List<String> questionsToAnswer = questions != null ? questions : 
            (singleQuery != null ? Collections.singletonList(singleQuery) : Collections.emptyList());
        
        for (String question : questionsToAnswer) {
            QAResult result = new QAResult();
            result.setQuestion(question);
            
            // Simple keyword-based answer extraction
            String answer = findBestAnswer(fullText, question);
            result.setAnswer(answer != null ? answer : "Unable to find a specific answer in the document.");
            result.setConfidence(answer != null ? 0.7 : 0.3);
            
            results.add(result);
        }
        
        return results;
    }
    
    private String findBestAnswer(String text, String question) {
        // Extract key terms from question
        String[] questionWords = question.toLowerCase()
            .replaceAll("[?.,!]", "")
            .split("\\s+");
        
        // Filter out common words
        Set<String> stopWords = Set.of("what", "is", "the", "a", "an", "are", "was", "were", "how", "when", "where", "who", "why", "can", "do", "does");
        List<String> keywords = Arrays.stream(questionWords)
            .filter(w -> !stopWords.contains(w) && w.length() > 2)
            .collect(Collectors.toList());
        
        if (keywords.isEmpty()) {
            return null;
        }
        
        // Find sentences containing keywords
        String[] sentences = text.split("[.!?]+");
        String bestMatch = null;
        int bestScore = 0;
        
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase();
            int score = 0;
            
            for (String keyword : keywords) {
                if (lowerSentence.contains(keyword)) {
                    score++;
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = sentence.trim();
            }
        }
        
        return bestScore >= Math.min(2, keywords.size()) ? bestMatch : null;
    }
    
    /**
     * Generate document summary.
     */
    private void performSummarization(String fullText, AnalysisResult result, 
                                       AIAnalysisRequest.SummaryOptions options) {
        int maxLength = options != null ? options.getMaxLength() : 500;
        
        // Extract sentences
        String[] sentences = fullText.split("[.!?]+");
        
        // Score sentences by importance (position, length, keyword density)
        List<Map.Entry<String, Double>> scoredSentences = new ArrayList<>();
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 20) continue;
            
            double score = scoreSentence(sentence, i, sentences.length, fullText);
            scoredSentences.add(Map.entry(sentence, score));
        }
        
        // Sort by score and build summary
        StringBuilder summary = new StringBuilder();
        int wordCount = 0;
        
        List<String> topSentences = scoredSentences.stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String sentence : topSentences) {
            int sentenceWords = sentence.split("\\s+").length;
            if (wordCount + sentenceWords > maxLength) break;
            
            if (summary.length() > 0) summary.append(" ");
            summary.append(sentence).append(".");
            wordCount += sentenceWords;
        }
        
        result.setSummary(summary.toString());
        
        // Extract keywords
        if (options == null || options.isIncludeKeywords()) {
            result.setKeywords(extractKeywords(fullText, options != null ? options.getTopKeywordsCount() : 10));
        }
    }
    
    private double scoreSentence(String sentence, int position, int totalSentences, String fullText) {
        double score = 0;
        
        // Position score (earlier sentences often more important)
        if (position < 5) score += 0.3;
        else if (position < totalSentences * 0.2) score += 0.2;
        
        // Length score (moderate length sentences preferred)
        int words = sentence.split("\\s+").length;
        if (words >= 10 && words <= 30) score += 0.2;
        
        // Keyword density
        String lower = sentence.toLowerCase();
        if (lower.contains("important") || lower.contains("significant") || 
            lower.contains("conclusion") || lower.contains("result")) {
            score += 0.3;
        }
        
        return score;
    }
    
    private List<String> extractKeywords(String text, int count) {
        // Simple keyword extraction using word frequency
        Map<String, Integer> wordFreq = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");
        
        Set<String> stopWords = Set.of("the", "a", "an", "is", "are", "was", "were", "be", "been", 
            "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", 
            "may", "might", "must", "shall", "can", "need", "dare", "ought", "used", "to", "of", 
            "in", "for", "on", "with", "at", "by", "from", "as", "into", "through", "during", 
            "before", "after", "above", "below", "between", "under", "again", "further", "then", 
            "once", "here", "there", "when", "where", "why", "how", "all", "each", "few", "more", 
            "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", 
            "than", "too", "very", "just", "and", "but", "if", "or", "because", "until", "while",
            "this", "that", "these", "those", "it", "its");
        
        for (String word : words) {
            if (word.length() > 3 && !stopWords.contains(word)) {
                wordFreq.merge(word, 1, Integer::sum);
            }
        }
        
        return wordFreq.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Extract named entities from document.
     */
    private List<Entity> extractEntities(Map<Integer, String> pageTexts, List<String> entityTypes) {
        List<Entity> entities = new ArrayList<>();
        Set<String> requestedTypes = entityTypes != null ? new HashSet<>(entityTypes) : null;
        
        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNumber = entry.getKey();
            String text = entry.getValue();
            
            // Extract emails
            if (requestedTypes == null || requestedTypes.contains("EMAIL")) {
                extractPattern(text, EMAIL_PATTERN, "EMAIL", pageNumber, entities);
            }
            
            // Extract phone numbers
            if (requestedTypes == null || requestedTypes.contains("PHONE")) {
                extractPattern(text, PHONE_PATTERN, "PHONE", pageNumber, entities);
            }
            
            // Extract dates
            if (requestedTypes == null || requestedTypes.contains("DATE")) {
                extractPattern(text, DATE_PATTERN, "DATE", pageNumber, entities);
            }
            
            // Extract money amounts
            if (requestedTypes == null || requestedTypes.contains("MONEY")) {
                extractPattern(text, MONEY_PATTERN, "MONEY", pageNumber, entities);
            }
            
            // Extract URLs
            if (requestedTypes == null || requestedTypes.contains("URL")) {
                extractPattern(text, URL_PATTERN, "URL", pageNumber, entities);
            }
        }
        
        // Deduplicate entities
        return entities.stream()
            .collect(Collectors.toMap(
                Entity::getText,
                e -> e,
                (e1, e2) -> e1.getConfidence() >= e2.getConfidence() ? e1 : e2
            ))
            .values()
            .stream()
            .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
            .collect(Collectors.toList());
    }
    
    private void extractPattern(String text, Pattern pattern, String type, int pageNumber, List<Entity> entities) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            Entity entity = new Entity();
            entity.setText(matcher.group());
            entity.setType(type);
            entity.setPageNumber(pageNumber);
            entity.setStartOffset(matcher.start());
            entity.setEndOffset(matcher.end());
            entity.setConfidence(0.9);
            entities.add(entity);
        }
    }
    
    /**
     * Categorize document type.
     */
    private List<Category> categorizeDocument(String fullText, AIAnalysisRequest.CategoryOptions options) {
        String lowerText = fullText.toLowerCase();
        List<Category> categories = new ArrayList<>();
        
        // Use custom categories if provided
        List<String> categoriesToCheck = options != null && options.getCustomCategories() != null ? 
            options.getCustomCategories() : new ArrayList<>(CATEGORY_KEYWORDS.keySet());
        
        float threshold = options != null ? options.getThreshold() : 0.5f;
        
        for (String categoryName : categoriesToCheck) {
            List<String> keywords = CATEGORY_KEYWORDS.get(categoryName);
            if (keywords == null) continue;
            
            // Count keyword matches
            int matches = 0;
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    matches++;
                }
            }
            
            double confidence = (double) matches / keywords.size();
            
            if (confidence >= threshold) {
                Category category = new Category();
                category.setName(categoryName);
                category.setConfidence(confidence);
                categories.add(category);
            }
        }
        
        // Sort by confidence
        categories.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        // If multiLabel is false, return only top category
        if (options != null && !options.isMultiLabel() && !categories.isEmpty()) {
            return Collections.singletonList(categories.get(0));
        }
        
        return categories;
    }
    
    /**
     * Extract key points from document.
     */
    private List<String> extractKeyPoints(String fullText) {
        List<String> keyPoints = new ArrayList<>();
        
        // Look for bullet points, numbered lists
        Pattern bulletPattern = Pattern.compile("^\\s*[•●○◦\\-\\*]\\s*(.+)$", Pattern.MULTILINE);
        Pattern numberPattern = Pattern.compile("^\\s*\\d+[.)]\\s*(.+)$", Pattern.MULTILINE);
        
        Matcher bulletMatcher = bulletPattern.matcher(fullText);
        while (bulletMatcher.find() && keyPoints.size() < 20) {
            String point = bulletMatcher.group(1).trim();
            if (point.length() > 10 && point.length() < 200) {
                keyPoints.add(point);
            }
        }
        
        Matcher numberMatcher = numberPattern.matcher(fullText);
        while (numberMatcher.find() && keyPoints.size() < 20) {
            String point = numberMatcher.group(1).trim();
            if (point.length() > 10 && point.length() < 200) {
                keyPoints.add(point);
            }
        }
        
        // If no bullet points found, extract important sentences
        if (keyPoints.isEmpty()) {
            String[] sentences = fullText.split("[.!?]+");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                String lower = trimmed.toLowerCase();
                
                if (trimmed.length() > 30 && trimmed.length() < 200) {
                    if (lower.contains("important") || lower.contains("key") || 
                        lower.contains("significant") || lower.contains("main") ||
                        lower.contains("primary") || lower.contains("essential")) {
                        keyPoints.add(trimmed);
                        if (keyPoints.size() >= 10) break;
                    }
                }
            }
        }
        
        return keyPoints;
    }
    
    /**
     * Extract tables from document (basic detection).
     */
    private List<ExtractedTable> extractTables(Map<Integer, String> pageTexts) {
        List<ExtractedTable> tables = new ArrayList<>();
        
        for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
            int pageNumber = entry.getKey();
            String text = entry.getValue();
            
            // Look for tabular patterns (rows with consistent delimiters)
            String[] lines = text.split("\n");
            List<String[]> currentTable = new ArrayList<>();
            int expectedColumns = -1;
            
            for (String line : lines) {
                // Check for tab-delimited or multiple-space-delimited content
                String[] cells = line.split("\\t|\\s{2,}");
                
                if (cells.length >= 2) {
                    if (expectedColumns == -1) {
                        expectedColumns = cells.length;
                        currentTable.add(cells);
                    } else if (cells.length == expectedColumns) {
                        currentTable.add(cells);
                    } else if (!currentTable.isEmpty() && currentTable.size() >= 2) {
                        // Save current table and start new one
                        tables.add(buildExtractedTable(currentTable, pageNumber, tables.size()));
                        currentTable = new ArrayList<>();
                        expectedColumns = -1;
                    }
                } else if (!currentTable.isEmpty() && currentTable.size() >= 2) {
                    tables.add(buildExtractedTable(currentTable, pageNumber, tables.size()));
                    currentTable = new ArrayList<>();
                    expectedColumns = -1;
                }
            }
            
            // Don't forget last table on page
            if (currentTable.size() >= 2) {
                tables.add(buildExtractedTable(currentTable, pageNumber, tables.size()));
            }
        }
        
        return tables;
    }
    
    private ExtractedTable buildExtractedTable(List<String[]> rows, int pageNumber, int tableIndex) {
        ExtractedTable table = new ExtractedTable();
        table.setPageNumber(pageNumber);
        table.setTableIndex(tableIndex);
        table.setRowCount(rows.size());
        table.setColumnCount(rows.isEmpty() ? 0 : rows.get(0).length);
        
        // First row as headers
        if (!rows.isEmpty()) {
            table.setHeaders(Arrays.asList(rows.get(0)));
        }
        
        // All rows as data
        List<List<String>> data = rows.stream()
            .map(Arrays::asList)
            .collect(Collectors.toList());
        table.setData(data);
        
        return table;
    }
}
