package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.request.FormCreateRequest;
import com.chnindia.eighteenpluspdf.dto.request.FormExportRequest;
import com.chnindia.eighteenpluspdf.dto.request.FormFillRequest;
import com.chnindia.eighteenpluspdf.dto.response.FormDataResponse;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for PDF form operations.
 * Handles form filling, creation, data extraction, and form field detection.
 */
@Service
public class FormService {
    
    private static final Logger logger = LoggerFactory.getLogger(FormService.class);
    
    @Autowired
    private FileUtil fileUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Fill form fields in a PDF document.
     */
    public Map<String, Object> fillForm(Path inputFile, FormFillRequest request) {
        logger.info("Filling form in PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            if (acroForm == null) {
                throw new PDFProcessingException("NO_FORM", "PDF does not contain a form");
            }
            
            Map<String, Object> formData = request.getFormData();
            int filledCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : formData.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    PDField field = acroForm.getField(fieldName);
                    if (field == null) {
                        errors.add("Field not found: " + fieldName);
                        continue;
                    }
                    
                    if (field.isReadOnly() && request.isPreserveReadOnly()) {
                        errors.add("Field is read-only: " + fieldName);
                        continue;
                    }
                    
                    fillField(field, value, request);
                    filledCount++;
                    
                } catch (Exception e) {
                    errors.add("Error filling field " + fieldName + ": " + e.getMessage());
                }
            }
            
            // Flatten if requested
            if (request.isFlattenAfterFill()) {
                acroForm.flatten();
            }
            
            // Save the document
            String outputName = request.getOutputFileName();
            if (outputName == null || outputName.trim().isEmpty()) {
                outputName = "filled_form";
            }
            
            Path outputPath = fileUtil.createOutputFile(outputName, "pdf");
            document.save(outputPath.toFile());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "filledFields", filledCount,
                "totalFieldsRequested", formData.size(),
                "errors", errors,
                "flattened", request.isFlattenAfterFill()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("FORM_FILL_ERROR", "Failed to fill form: " + e.getMessage());
        }
    }
    
    private void fillField(PDField field, Object value, FormFillRequest request) throws IOException {
        if (field instanceof PDTextField textField) {
            textField.setValue(String.valueOf(value));
        } else if (field instanceof PDCheckBox checkBox) {
            if (value instanceof Boolean) {
                if ((Boolean) value) {
                    checkBox.check();
                } else {
                    checkBox.unCheck();
                }
            } else {
                String strValue = String.valueOf(value).toLowerCase();
                if ("true".equals(strValue) || "yes".equals(strValue) || "1".equals(strValue)) {
                    checkBox.check();
                } else {
                    checkBox.unCheck();
                }
            }
        } else if (field instanceof PDRadioButton radioButton) {
            radioButton.setValue(String.valueOf(value));
        } else if (field instanceof PDComboBox comboBox) {
            comboBox.setValue(String.valueOf(value));
        } else if (field instanceof PDListBox listBox) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) value;
                listBox.setValue(values);
            } else {
                listBox.setValue(Collections.singletonList(String.valueOf(value)));
            }
        } else if (field instanceof PDSignatureField) {
            // Signature fields require special handling
            logger.warn("Signature field {} cannot be filled directly", field.getFullyQualifiedName());
        } else if (field instanceof PDNonTerminalField nonTerminalField) {
            // Handle field hierarchy
            for (PDField child : nonTerminalField.getChildren()) {
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> childValues = (Map<String, Object>) value;
                    Object childValue = childValues.get(child.getPartialName());
                    if (childValue != null) {
                        fillField(child, childValue, request);
                    }
                }
            }
        }
    }
    
    /**
     * Extract form data from a PDF document.
     */
    public FormDataResponse extractFormData(Path inputFile, FormExportRequest request) {
        logger.info("Extracting form data from PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            FormDataResponse response = new FormDataResponse();
            response.setExtractedAt(LocalDateTime.now());
            response.setFormat(request.getFormat().name());
            
            if (acroForm == null) {
                response.setFieldCount(0);
                response.setFields(Collections.emptyList());
                response.setFormData(Collections.emptyMap());
                return response;
            }
            
            List<FormDataResponse.FormField> fields = new ArrayList<>();
            Map<String, Object> formData = new LinkedHashMap<>();
            FormDataResponse.FormStatistics stats = new FormDataResponse.FormStatistics();
            
            int totalFields = 0, filledFields = 0, textFields = 0, checkboxFields = 0;
            int radioFields = 0, dropdownFields = 0, signatureFields = 0;
            
            for (PDField field : acroForm.getFieldTree()) {
                if (field instanceof PDNonTerminalField) {
                    continue; // Skip container fields
                }
                
                FormDataResponse.FormField formField = new FormDataResponse.FormField();
                formField.setName(field.getFullyQualifiedName());
                formField.setType(getFieldType(field));
                formField.setValue(field.getValueAsString());
                formField.setReadOnly(field.isReadOnly());
                formField.setRequired(field.isRequired());
                formField.setHasValue(!field.getValueAsString().isEmpty());
                
                // Get options for choice fields
                if (field instanceof PDChoice choiceField) {
                    formField.setOptions(choiceField.getOptions());
                }
                
                // Add metadata if requested
                if (request.isIncludeFieldMetadata()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("alternateFieldName", field.getAlternateFieldName());
                    metadata.put("mappingName", field.getMappingName());
                    formField.setMetadata(metadata);
                }
                
                // Include empty fields if requested
                if (request.isIncludeEmptyFields() || formField.isHasValue()) {
                    fields.add(formField);
                    formData.put(formField.getName(), formField.getValue());
                }
                
                // Update statistics
                totalFields++;
                if (formField.isHasValue()) filledFields++;
                
                if (field instanceof PDTextField) textFields++;
                else if (field instanceof PDCheckBox) checkboxFields++;
                else if (field instanceof PDRadioButton) radioFields++;
                else if (field instanceof PDComboBox || field instanceof PDListBox) dropdownFields++;
                else if (field instanceof PDSignatureField) signatureFields++;
            }
            
            stats.setTotalFields(totalFields);
            stats.setFilledFields(filledFields);
            stats.setEmptyFields(totalFields - filledFields);
            stats.setTextFields(textFields);
            stats.setCheckboxFields(checkboxFields);
            stats.setRadioFields(radioFields);
            stats.setDropdownFields(dropdownFields);
            stats.setSignatureFields(signatureFields);
            stats.setCompletionPercentage(totalFields > 0 ? (filledFields * 100.0 / totalFields) : 0);
            
            response.setFieldCount(totalFields);
            response.setFields(fields);
            response.setFormData(formData);
            response.setStatistics(stats);
            
            // Export to file if requested
            if (request.getOutputFileName() != null) {
                Path exportPath = exportFormData(formData, fields, request);
                response.setExportUrl(fileUtil.getDownloadUrl(exportPath.getFileName().toString()));
            }
            
            return response;
            
        } catch (IOException e) {
            throw new PDFProcessingException("FORM_EXTRACT_ERROR", "Failed to extract form data: " + e.getMessage());
        }
    }
    
    private Path exportFormData(Map<String, Object> formData, List<FormDataResponse.FormField> fields, 
                                 FormExportRequest request) throws IOException {
        String content;
        String extension;
        
        switch (request.getFormat()) {
            case JSON:
                content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formData);
                extension = "json";
                break;
            case XML:
                content = buildXML(formData);
                extension = "xml";
                break;
            case CSV:
                content = buildCSV(fields);
                extension = "csv";
                break;
            case FDF:
                content = buildFDF(formData);
                extension = "fdf";
                break;
            case XFDF:
                content = buildXFDF(formData);
                extension = "xfdf";
                break;
            default:
                throw new PDFProcessingException("UNSUPPORTED_FORMAT", "Unsupported export format: " + request.getFormat());
        }
        
        Path outputPath = fileUtil.createOutputFile(request.getOutputFileName(), extension);
        Files.writeString(outputPath, content);
        return outputPath;
    }
    
    private String buildXML(Map<String, Object> formData) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<form-data>\n");
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            xml.append("  <field name=\"").append(escapeXml(entry.getKey())).append("\">");
            xml.append(escapeXml(String.valueOf(entry.getValue())));
            xml.append("</field>\n");
        }
        xml.append("</form-data>");
        return xml.toString();
    }
    
    private String buildCSV(List<FormDataResponse.FormField> fields) {
        StringBuilder csv = new StringBuilder();
        csv.append("Field Name,Type,Value,Required,Read Only\n");
        for (FormDataResponse.FormField field : fields) {
            csv.append(escapeCSV(field.getName())).append(",");
            csv.append(escapeCSV(field.getType())).append(",");
            csv.append(escapeCSV(field.getValue())).append(",");
            csv.append(field.isRequired()).append(",");
            csv.append(field.isReadOnly()).append("\n");
        }
        return csv.toString();
    }
    
    private String buildFDF(Map<String, Object> formData) {
        StringBuilder fdf = new StringBuilder();
        fdf.append("%FDF-1.2\n");
        fdf.append("1 0 obj\n");
        fdf.append("<< /FDF << /Fields [\n");
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            fdf.append("<< /T (").append(entry.getKey()).append(") /V (").append(entry.getValue()).append(") >>\n");
        }
        fdf.append("] >> >>\n");
        fdf.append("endobj\n");
        fdf.append("trailer\n");
        fdf.append("<< /Root 1 0 R >>\n");
        fdf.append("%%EOF");
        return fdf.toString();
    }
    
    private String buildXFDF(Map<String, Object> formData) {
        StringBuilder xfdf = new StringBuilder();
        xfdf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xfdf.append("<xfdf xmlns=\"http://ns.adobe.com/xfdf/\" xml:space=\"preserve\">\n");
        xfdf.append("  <fields>\n");
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            xfdf.append("    <field name=\"").append(escapeXml(entry.getKey())).append("\">\n");
            xfdf.append("      <value>").append(escapeXml(String.valueOf(entry.getValue()))).append("</value>\n");
            xfdf.append("    </field>\n");
        }
        xfdf.append("  </fields>\n");
        xfdf.append("</xfdf>");
        return xfdf.toString();
    }
    
    private String getFieldType(PDField field) {
        if (field instanceof PDTextField) return "TEXT";
        if (field instanceof PDCheckBox) return "CHECKBOX";
        if (field instanceof PDRadioButton) return "RADIO";
        if (field instanceof PDComboBox) return "DROPDOWN";
        if (field instanceof PDListBox) return "LISTBOX";
        if (field instanceof PDSignatureField) return "SIGNATURE";
        if (field instanceof PDPushButton) return "BUTTON";
        return "UNKNOWN";
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    private String escapeCSV(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
    
    /**
     * Create a new PDF form with specified fields.
     */
    public Map<String, Object> createForm(FormCreateRequest request) {
        logger.info("Creating new PDF form with {} fields", request.getFields().size());
        
        try (PDDocument document = new PDDocument()) {
            // Create page(s)
            PDRectangle pageSize = getPageSize(request.getPageLayout());
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            
            // Create AcroForm
            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            
            // Set default resources
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            acroForm.setDefaultResources(new org.apache.pdfbox.pdmodel.PDResources());
            acroForm.getDefaultResources().put(COSName.getPDFName("Helv"), font);
            
            // Create fields
            List<PDField> fields = new ArrayList<>();
            for (FormCreateRequest.FormFieldDefinition fieldDef : request.getFields()) {
                PDField field = createField(document, acroForm, fieldDef, page);
                if (field != null) {
                    fields.add(field);
                }
            }
            
            acroForm.setFields(fields);
            
            // Save the document
            String outputName = request.getOutputFileName();
            if (outputName == null || outputName.trim().isEmpty()) {
                outputName = "new_form";
            }
            
            Path outputPath = fileUtil.createOutputFile(outputName, "pdf");
            document.save(outputPath.toFile());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "fieldCount", fields.size(),
                "pageCount", document.getNumberOfPages(),
                "fileSize", fileUtil.getHumanReadableSize(outputPath)
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("FORM_CREATE_ERROR", "Failed to create form: " + e.getMessage());
        }
    }
    
    private PDRectangle getPageSize(FormCreateRequest.PageLayout layout) {
        String size = layout.getPageSize().toUpperCase();
        PDRectangle baseSize;
        
        switch (size) {
            case "LETTER":
                baseSize = PDRectangle.LETTER;
                break;
            case "LEGAL":
                baseSize = PDRectangle.LEGAL;
                break;
            case "CUSTOM":
                baseSize = new PDRectangle(layout.getCustomWidth(), layout.getCustomHeight());
                break;
            case "A4":
            default:
                baseSize = PDRectangle.A4;
                break;
        }
        
        if ("LANDSCAPE".equalsIgnoreCase(layout.getOrientation())) {
            return new PDRectangle(baseSize.getHeight(), baseSize.getWidth());
        }
        
        return baseSize;
    }
    
    private PDField createField(PDDocument document, PDAcroForm acroForm, 
                                  FormCreateRequest.FormFieldDefinition fieldDef, PDPage page) throws IOException {
        PDField field;
        
        switch (fieldDef.getType()) {
            case TEXT:
            case DATE:
            case NUMBER:
                field = new PDTextField(acroForm);
                if (fieldDef.getMaxLength() > 0) {
                    ((PDTextField) field).setMaxLen(fieldDef.getMaxLength());
                }
                ((PDTextField) field).setMultiline(fieldDef.isMultiline());
                break;
            case CHECKBOX:
                field = new PDCheckBox(acroForm);
                break;
            case RADIO:
                field = new PDRadioButton(acroForm);
                break;
            case DROPDOWN:
                PDComboBox comboBox = new PDComboBox(acroForm);
                if (fieldDef.getOptions() != null) {
                    comboBox.setOptions(fieldDef.getOptions());
                }
                field = comboBox;
                break;
            case LISTBOX:
                PDListBox listBox = new PDListBox(acroForm);
                if (fieldDef.getOptions() != null) {
                    listBox.setOptions(fieldDef.getOptions());
                }
                field = listBox;
                break;
            case SIGNATURE:
                field = new PDSignatureField(acroForm);
                break;
            case BUTTON:
                field = new PDPushButton(acroForm);
                break;
            default:
                logger.warn("Unsupported field type: {}", fieldDef.getType());
                return null;
        }
        
        field.setPartialName(fieldDef.getName());
        field.setReadOnly(fieldDef.isReadOnly());
        field.setRequired(fieldDef.isRequired());
        
        if (fieldDef.getTooltip() != null) {
            field.setAlternateFieldName(fieldDef.getTooltip());
        }
        
        // Set default value
        if (fieldDef.getDefaultValue() != null && field instanceof PDTextField) {
            ((PDTextField) field).setDefaultValue(fieldDef.getDefaultValue());
            field.setValue(fieldDef.getDefaultValue());
        }
        
        // Create widget annotation
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setRectangle(new PDRectangle(
            fieldDef.getX(), 
            fieldDef.getY(), 
            fieldDef.getWidth(), 
            fieldDef.getHeight()
        ));
        widget.setPage(page);
        
        field.getWidgets().add(widget);
        page.getAnnotations().add(widget);
        
        return field;
    }
    
    /**
     * Detect form fields in a PDF document.
     */
    public Map<String, Object> detectFormFields(Path inputFile) {
        logger.info("Detecting form fields in PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            
            List<Map<String, Object>> detectedFields = new ArrayList<>();
            
            if (acroForm != null) {
                for (PDField field : acroForm.getFieldTree()) {
                    if (field instanceof PDNonTerminalField) {
                        continue;
                    }
                    
                    Map<String, Object> fieldInfo = new LinkedHashMap<>();
                    fieldInfo.put("name", field.getFullyQualifiedName());
                    fieldInfo.put("type", getFieldType(field));
                    fieldInfo.put("value", field.getValueAsString());
                    fieldInfo.put("required", field.isRequired());
                    fieldInfo.put("readOnly", field.isReadOnly());
                    
                    // Get widget position
                    if (!field.getWidgets().isEmpty()) {
                        PDAnnotationWidget widget = field.getWidgets().get(0);
                        PDRectangle rect = widget.getRectangle();
                        if (rect != null) {
                            fieldInfo.put("x", rect.getLowerLeftX());
                            fieldInfo.put("y", rect.getLowerLeftY());
                            fieldInfo.put("width", rect.getWidth());
                            fieldInfo.put("height", rect.getHeight());
                        }
                    }
                    
                    // Get options for choice fields
                    if (field instanceof PDChoice choiceField) {
                        fieldInfo.put("options", choiceField.getOptions());
                    }
                    
                    detectedFields.add(fieldInfo);
                }
            }
            
            return Map.of(
                "hasForm", acroForm != null,
                "fieldCount", detectedFields.size(),
                "fields", detectedFields
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("FORM_DETECT_ERROR", "Failed to detect form fields: " + e.getMessage());
        }
    }
    
    /**
     * Import form data from FDF/XFDF file.
     */
    public Map<String, Object> importFormData(Path pdfFile, Path dataFile, String format) {
        logger.info("Importing form data from {} into {}", dataFile, pdfFile);
        
        try {
            Map<String, Object> formData;
            String content = Files.readString(dataFile);
            
            switch (format.toUpperCase()) {
                case "JSON":
                    formData = objectMapper.readValue(content, Map.class);
                    break;
                case "FDF":
                case "XFDF":
                case "XML":
                    formData = parseXmlFormData(content);
                    break;
                default:
                    throw new PDFProcessingException("UNSUPPORTED_FORMAT", "Unsupported import format: " + format);
            }
            
            FormFillRequest fillRequest = new FormFillRequest();
            fillRequest.setFormData(formData);
            
            return fillForm(pdfFile, fillRequest);
            
        } catch (IOException e) {
            throw new PDFProcessingException("IMPORT_ERROR", "Failed to import form data: " + e.getMessage());
        }
    }
    
    private Map<String, Object> parseXmlFormData(String xml) {
        Map<String, Object> data = new LinkedHashMap<>();
        // Simple XML parsing for field/value pairs
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<field\\s+name=\"([^\"]+)\"[^>]*>\\s*(?:<value>)?([^<]*)(?:</value>)?\\s*</field>",
            java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            data.put(matcher.group(1), matcher.group(2).trim());
        }
        return data;
    }
}
