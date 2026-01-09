package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.request.*;
import com.chnindia.eighteenpluspdf.dto.response.PDFProcessingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Base interface for all PDF processing services
 */
public interface PDFProcessingService {
    
    /**
     * Process a PDF operation
     * @param request The request DTO containing file and parameters
     * @return Response with job ID or direct result
     */
    PDFProcessingResponse process(Object request);
    
    /**
     * Get the tool name for this service
     * @return Tool name identifier
     */
    String getToolName();
    
    /**
     * Validate request parameters
     * @param request The request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateRequest(Object request);
}

/**
 * Interface for merge operations
 */
interface MergePDFService extends PDFProcessingService {
    PDFProcessingResponse merge(MergePDFRequest request);
}

/**
 * Interface for split operations
 */
interface SplitPDFService extends PDFProcessingService {
    PDFProcessingResponse split(SplitPDFRequest request);
}

/**
 * Interface for compress operations
 */
interface CompressPDFService extends PDFProcessingService {
    PDFProcessingResponse compress(CompressPDFRequest request);
}

/**
 * Interface for rotate operations
 */
interface RotatePDFService extends PDFProcessingService {
    PDFProcessingResponse rotate(RotatePDFRequest request);
}

/**
 * Interface for watermark operations
 */
interface WatermarkPDFService extends PDFProcessingService {
    PDFProcessingResponse watermark(WatermarkPDFRequest request);
}

/**
 * Interface for encrypt operations
 */
interface EncryptPDFService extends PDFProcessingService {
    PDFProcessingResponse encrypt(EncryptPDFRequest request);
}

/**
 * Interface for decrypt operations
 */
interface DecryptPDFService extends PDFProcessingService {
    PDFProcessingResponse decrypt(DecryptPDFRequest request);
}

/**
 * Interface for text extraction
 */
interface ExtractTextService extends PDFProcessingService {
    PDFProcessingResponse extractText(MultipartFile file);
}

/**
 * Interface for image extraction
 */
interface ExtractImagesService extends PDFProcessingService {
    PDFProcessingResponse extractImages(MultipartFile file);
}

/**
 * Interface for metadata extraction
 */
interface ExtractMetadataService extends PDFProcessingService {
    PDFProcessingResponse extractMetadata(MultipartFile file);
}

/**
 * Interface for page numbers
 */
interface AddPageNumbersService extends PDFProcessingService {
    PDFProcessingResponse addPageNumbers(PageNumbersRequest request);
}

/**
 * Interface for page removal
 */
interface RemovePagesService extends PDFProcessingService {
    PDFProcessingResponse removePages(RemovePagesRequest request);
}

/**
 * Interface for crop pages
 */
interface CropPagesService extends PDFProcessingService {
    PDFProcessingResponse cropPages(CropPagesRequest request);
}

/**
 * Interface for resize pages
 */
interface ResizePagesService extends PDFProcessingService {
    PDFProcessingResponse resizePages(ResizePagesRequest request);
}

/**
 * Interface for PDF to image conversion
 */
interface PDFToImageService extends PDFProcessingService {
    PDFProcessingResponse pdfToImage(PDFToImageRequest request);
}

/**
 * Interface for image to PDF conversion
 */
interface ImageToPDFService extends PDFProcessingService {
    PDFProcessingResponse imageToPDF(ImageToPDFRequest request);
}

/**
 * Interface for PDF to text conversion
 */
interface PDFToTextService extends PDFProcessingService {
    PDFProcessingResponse pdfToText(PDFToTextRequest request);
}

/**
 * Interface for text to PDF conversion
 */
interface TextToPDFService extends PDFProcessingService {
    PDFProcessingResponse textToPDF(TextToPDFRequest request);
}

/**
 * Interface for Office to PDF conversion
 */
interface OfficeToPDFService extends PDFProcessingService {
    PDFProcessingResponse officeToPDF(OfficeToPDFRequest request);
}

/**
 * Interface for HTML to PDF conversion
 */
interface HTMLToPDFService extends PDFProcessingService {
    PDFProcessingResponse htmlToPDF(HTMLToPDFRequest request);
}

/**
 * Interface for Markdown to PDF conversion
 */
interface MarkdownToPDFService extends PDFProcessingService {
    PDFProcessingResponse markdownToPDF(MarkdownToPDFRequest request);
}

/**
 * Interface for OCR processing
 */
interface OCRPDFService extends PDFProcessingService {
    PDFProcessingResponse ocrPDF(OCRPDFRequest request);
}

/**
 * Interface for PDF comparison
 */
interface ComparePDFsService extends PDFProcessingService {
    PDFProcessingResponse comparePDFs(ComparePDFsRequest request);
}

/**
 * Interface for PDF/A conversion
 */
interface PDFAConvertService extends PDFProcessingService {
    PDFProcessingResponse pdfaConvert(PDFAConvertRequest request);
}

/**
 * Interface for linearization
 */
interface LinearizeService extends PDFProcessingService {
    PDFProcessingResponse linearize(LinearizeRequest request);
}

/**
 * Interface for optimization
 */
interface OptimizeService extends PDFProcessingService {
    PDFProcessingResponse optimize(OptimizeRequest request);
}

/**
 * Interface for metadata editing
 */
interface MetadataEditService extends PDFProcessingService {
    PDFProcessingResponse editMetadata(MetadataEditRequest request);
}