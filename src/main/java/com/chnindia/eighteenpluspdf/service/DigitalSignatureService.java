package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Enterprise-grade Digital Signature Service.
 * Full PKI/X.509, PKCS12, timestamping, multi-signature, and verification.
 */
@Service
public class DigitalSignatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(DigitalSignatureService.class);
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    @Autowired
    private FileUtil fileUtil;
    
    @Value("${app.signature.tsa-url:http://timestamp.digicert.com}")
    private String tsaUrl;
    
    @Value("${app.signature.default-algorithm:SHA256withRSA}")
    private String defaultAlgorithm;
    
    /**
     * Sign PDF with PKCS12 certificate (full PKI implementation).
     */
    public Map<String, Object> signPDF(Path inputFile, Path outputFile, SignatureConfig config) {
        logger.info("Signing PDF with PKI certificate: {}", inputFile);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Load keystore
            KeyStore keystore = loadKeyStore(config);
            
            // Get private key and certificate chain
            String alias = config.getKeyAlias();
            if (alias == null) {
                Enumeration<String> aliases = keystore.aliases();
                if (aliases.hasMoreElements()) {
                    alias = aliases.nextElement();
                }
            }
            
            PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, config.getKeyPassword().toCharArray());
            Certificate[] certChain = keystore.getCertificateChain(alias);
            
            if (privateKey == null || certChain == null) {
                throw new PDFProcessingException("SIGNATURE_ERROR", "Could not load certificate from keystore");
            }
            
            // Convert to X509Certificate array
            X509Certificate[] x509Chain = Arrays.stream(certChain)
                .map(c -> (X509Certificate) c)
                .toArray(X509Certificate[]::new);
            
            // Create signature handler
            SignatureHandler signatureHandler = new SignatureHandler(privateKey, x509Chain, 
                config.getAlgorithm() != null ? config.getAlgorithm() : defaultAlgorithm,
                config.isIncludeTimestamp() ? tsaUrl : null);
            
            // Sign the document
            try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
                // Create signature dictionary
                PDSignature signature = new PDSignature();
                signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                signature.setName(config.getSignerName() != null ? config.getSignerName() : getSignerName(x509Chain[0]));
                signature.setLocation(config.getLocation());
                signature.setReason(config.getReason());
                signature.setSignDate(Calendar.getInstance());
                
                // Configure signature options
                SignatureOptions signatureOptions = new SignatureOptions();
                signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
                
                // Add visible signature if requested
                if (config.isVisibleSignature()) {
                    addVisibleSignature(document, signature, signatureOptions, config, x509Chain[0]);
                }
                
                // Register signature
                document.addSignature(signature, signatureHandler, signatureOptions);
                
                // Save signed document
                try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                    document.saveIncremental(fos);
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Build response
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("resultUrl", fileUtil.getDownloadUrl(outputFile.getFileName().toString()));
            result.put("signed", true);
            result.put("signerName", config.getSignerName() != null ? config.getSignerName() : getSignerName(x509Chain[0]));
            result.put("signatureAlgorithm", config.getAlgorithm() != null ? config.getAlgorithm() : defaultAlgorithm);
            result.put("certificateSubject", x509Chain[0].getSubjectX500Principal().getName());
            result.put("certificateIssuer", x509Chain[0].getIssuerX500Principal().getName());
            result.put("certificateValidFrom", x509Chain[0].getNotBefore().toString());
            result.put("certificateValidTo", x509Chain[0].getNotAfter().toString());
            result.put("timestamped", config.isIncludeTimestamp());
            result.put("visibleSignature", config.isVisibleSignature());
            result.put("processingTimeMs", processingTime);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to sign PDF", e);
            throw new PDFProcessingException("SIGNATURE_ERROR", "Failed to sign PDF: " + e.getMessage());
        }
    }
    
    /**
     * Verify all signatures in a PDF document.
     */
    public Map<String, Object> verifySignatures(Path inputFile) {
        logger.info("Verifying signatures in PDF: {}", inputFile);
        
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> signatureDetails = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            
            result.put("hasSignatures", !signatures.isEmpty());
            result.put("signatureCount", signatures.size());
            
            boolean allValid = true;
            
            for (int i = 0; i < signatures.size(); i++) {
                PDSignature signature = signatures.get(i);
                Map<String, Object> sigInfo = new LinkedHashMap<>();
                
                sigInfo.put("signatureIndex", i + 1);
                sigInfo.put("signerName", signature.getName());
                sigInfo.put("signDate", signature.getSignDate() != null ? 
                    signature.getSignDate().getTime().toString() : "Unknown");
                sigInfo.put("reason", signature.getReason());
                sigInfo.put("location", signature.getLocation());
                sigInfo.put("filter", signature.getFilter());
                sigInfo.put("subFilter", signature.getSubFilter());
                
                // Verify signature
                try {
                    byte[] signatureContent;
                    byte[] signedContent;
                    
                    // Read file content for signature verification
                    try (FileInputStream fis = new FileInputStream(inputFile.toFile())) {
                        byte[] fileBytes = fis.readAllBytes();
                        signatureContent = signature.getContents(fileBytes);
                        signedContent = signature.getSignedContent(fileBytes);
                    }
                    
                    CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(signedContent), signatureContent);
                    SignerInformationStore signerStore = signedData.getSignerInfos();
                    
                    Collection<SignerInformation> signers = signerStore.getSigners();
                    
                    for (SignerInformation signer : signers) {
                        @SuppressWarnings("unchecked")
                        Collection<X509CertificateHolder> certCollection = signedData.getCertificates().getMatches(signer.getSID());
                        
                        if (!certCollection.isEmpty()) {
                            X509CertificateHolder certHolder = certCollection.iterator().next();
                            X509Certificate cert = new JcaX509CertificateConverter()
                                .setProvider("BC")
                                .getCertificate(certHolder);
                            
                            // Verify signature
                            boolean verified = signer.verify(
                                new org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder()
                                    .setProvider("BC")
                                    .build(cert)
                            );
                            
                            sigInfo.put("valid", verified);
                            sigInfo.put("certificateSubject", cert.getSubjectX500Principal().getName());
                            sigInfo.put("certificateIssuer", cert.getIssuerX500Principal().getName());
                            sigInfo.put("certificateSerial", cert.getSerialNumber().toString());
                            
                            // Check certificate validity
                            try {
                                cert.checkValidity();
                                sigInfo.put("certificateValid", true);
                                sigInfo.put("certificateStatus", "VALID");
                            } catch (CertificateExpiredException e) {
                                sigInfo.put("certificateValid", false);
                                sigInfo.put("certificateStatus", "EXPIRED");
                                allValid = false;
                            } catch (CertificateNotYetValidException e) {
                                sigInfo.put("certificateValid", false);
                                sigInfo.put("certificateStatus", "NOT_YET_VALID");
                                allValid = false;
                            }
                            
                            if (!verified) allValid = false;
                        }
                    }
                    
                    // Check for timestamp
                    sigInfo.put("hasTimestamp", checkForTimestamp(signedData));
                    
                } catch (Exception e) {
                    sigInfo.put("valid", false);
                    sigInfo.put("error", e.getMessage());
                    allValid = false;
                }
                
                signatureDetails.add(sigInfo);
            }
            
            result.put("allSignaturesValid", allValid);
            result.put("signatures", signatureDetails);
            result.put("verifiedAt", LocalDateTime.now().toString());
            
        } catch (Exception e) {
            logger.error("Failed to verify signatures", e);
            result.put("error", e.getMessage());
            result.put("hasSignatures", false);
        }
        
        return result;
    }
    
    /**
     * Add multiple signatures to a document.
     */
    public Map<String, Object> addMultipleSignatures(Path inputFile, Path outputFile, List<SignatureConfig> configs) {
        logger.info("Adding {} signatures to PDF: {}", configs.size(), inputFile);
        
        Path currentFile = inputFile;
        int signedCount = 0;
        
        for (int i = 0; i < configs.size(); i++) {
            SignatureConfig config = configs.get(i);
            Path tempOutput = (i == configs.size() - 1) ? outputFile : 
                createTempFile("multisig_" + i, ".pdf");
            
            try {
                signPDF(currentFile, tempOutput, config);
                signedCount++;
                
                // Use the newly signed file as input for next signature
                if (i > 0 && !currentFile.equals(inputFile)) {
                    Files.deleteIfExists(currentFile);
                }
                currentFile = tempOutput;
                
            } catch (Exception e) {
                logger.error("Failed to add signature {}: {}", i + 1, e.getMessage());
            }
        }
        
        return Map.of(
            "resultUrl", fileUtil.getDownloadUrl(outputFile.getFileName().toString()),
            "signaturesAdded", signedCount,
            "totalRequested", configs.size()
        );
    }
    
    /**
     * Generate a self-signed certificate for testing.
     */
    public Map<String, Object> generateSelfSignedCertificate(String commonName, String organization, 
                                                             int validityDays, Path outputPath) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            // Build certificate
            org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name(
                "CN=" + commonName + ", O=" + organization);
            
            java.math.BigInteger serialNumber = java.math.BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date();
            Date notAfter = new Date(System.currentTimeMillis() + (long) validityDays * 24 * 60 * 60 * 1000);
            
            org.bouncycastle.cert.X509v3CertificateBuilder certBuilder = 
                new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                    subject, serialNumber, notBefore, notAfter, subject, keyPair.getPublic());
            
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
            
            X509CertificateHolder certHolder = certBuilder.build(contentSigner);
            X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
            
            // Create PKCS12 keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            keyStore.load(null, null);
            keyStore.setKeyEntry("signature", keyPair.getPrivate(), 
                "changeit".toCharArray(), new Certificate[]{certificate});
            
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                keyStore.store(fos, "changeit".toCharArray());
            }
            
            return Map.of(
                "certificatePath", outputPath.toString(),
                "commonName", commonName,
                "organization", organization,
                "validFrom", notBefore.toString(),
                "validTo", notAfter.toString(),
                "keyAlias", "signature",
                "keystorePassword", "changeit",
                "keyPassword", "changeit"
            );
            
        } catch (Exception e) {
            throw new PDFProcessingException("CERT_GEN_ERROR", "Failed to generate certificate: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private KeyStore loadKeyStore(SignatureConfig config) throws Exception {
        KeyStore keystore;
        
        if (config.getKeystoreType() != null && config.getKeystoreType().equalsIgnoreCase("PKCS11")) {
            keystore = KeyStore.getInstance("PKCS11");
            keystore.load(null, config.getKeystorePassword().toCharArray());
        } else {
            String keystoreType = config.getKeystoreType() != null ? config.getKeystoreType() : "PKCS12";
            keystore = KeyStore.getInstance(keystoreType);
            
            try (FileInputStream fis = new FileInputStream(config.getKeystorePath())) {
                keystore.load(fis, config.getKeystorePassword().toCharArray());
            }
        }
        
        return keystore;
    }
    
    private String getSignerName(X509Certificate cert) {
        String subject = cert.getSubjectX500Principal().getName();
        // Extract CN from subject
        for (String part : subject.split(",")) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return subject;
    }
    
    private void addVisibleSignature(PDDocument document, PDSignature signature, 
                                     SignatureOptions options, SignatureConfig config,
                                     X509Certificate cert) throws IOException {
        int pageNum = config.getSignaturePage() > 0 ? config.getSignaturePage() - 1 : 0;
        PDPage page = document.getPage(Math.min(pageNum, document.getNumberOfPages() - 1));
        
        float x = config.getSignatureX() > 0 ? config.getSignatureX() : 50;
        float y = config.getSignatureY() > 0 ? config.getSignatureY() : 50;
        float width = config.getSignatureWidth() > 0 ? config.getSignatureWidth() : 200;
        float height = config.getSignatureHeight() > 0 ? config.getSignatureHeight() : 50;
        
        // Create visible signature appearance
        PDRectangle rect = new PDRectangle(x, y, width, height);
        
        // Create appearance stream with signature details
        try (PDDocument tempDoc = new PDDocument()) {
            PDPage tempPage = new PDPage(new PDRectangle(width, height));
            tempDoc.addPage(tempPage);
            
            try (PDPageContentStream cs = new PDPageContentStream(tempDoc, tempPage)) {
                // Border
                cs.setStrokingColor(0, 0, 0);
                cs.setLineWidth(1);
                cs.addRect(1, 1, width - 2, height - 2);
                cs.stroke();
                
                // Signature text
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                cs.newLineAtOffset(5, height - 15);
                cs.showText("Digitally Signed by:");
                cs.endText();
                
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                cs.newLineAtOffset(5, height - 28);
                cs.showText(getSignerName(cert));
                cs.endText();
                
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                cs.newLineAtOffset(5, height - 40);
                cs.showText("Date: " + LocalDateTime.now().toString().substring(0, 19));
                cs.endText();
                
                if (config.getReason() != null) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    cs.newLineAtOffset(5, 8);
                    cs.showText("Reason: " + config.getReason());
                    cs.endText();
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            tempDoc.save(baos);
            options.setVisualSignature(new ByteArrayInputStream(baos.toByteArray()));
            options.setPage(pageNum);
        }
    }
    
    private boolean checkForTimestamp(CMSSignedData signedData) {
        for (SignerInformation signer : signedData.getSignerInfos().getSigners()) {
            if (signer.getUnsignedAttributes() != null) {
                org.bouncycastle.asn1.cms.Attribute tsAttr = signer.getUnsignedAttributes()
                    .get(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                if (tsAttr != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new PDFProcessingException("TEMP_ERROR", "Cannot create temp file");
        }
    }
    
    // ==================== SIGNATURE HANDLER ====================
    
    private static class SignatureHandler implements SignatureInterface {
        private final PrivateKey privateKey;
        private final X509Certificate[] certificateChain;
        private final String algorithm;
        private final String tsaUrl;
        
        public SignatureHandler(PrivateKey privateKey, X509Certificate[] certificateChain, 
                               String algorithm, String tsaUrl) {
            this.privateKey = privateKey;
            this.certificateChain = certificateChain;
            this.algorithm = algorithm;
            this.tsaUrl = tsaUrl;
        }
        
        @Override
        public byte[] sign(InputStream content) throws IOException {
            try {
                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                
                ContentSigner sha256Signer = new JcaContentSignerBuilder(algorithm)
                    .setProvider("BC")
                    .build(privateKey);
                
                gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                        .build(sha256Signer, certificateChain[0]));
                
                gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));
                
                CMSProcessableInputStream msg = new CMSProcessableInputStream(content);
                CMSSignedData signedData = gen.generate(msg, false);
                
                // Add timestamp if TSA URL is provided
                if (tsaUrl != null && !tsaUrl.isEmpty()) {
                    signedData = addTimestamp(signedData);
                }
                
                return signedData.getEncoded();
                
            } catch (Exception e) {
                throw new IOException("Cannot sign PDF", e);
            }
        }
        
        private CMSSignedData addTimestamp(CMSSignedData signedData) {
            try {
                SignerInformation signer = signedData.getSignerInfos().getSigners().iterator().next();
                
                // Get timestamp from TSA
                byte[] timestampToken = getTimestampToken(signer.getSignature());
                
                if (timestampToken != null) {
                    org.bouncycastle.asn1.ASN1Primitive asn1 = 
                        org.bouncycastle.asn1.ASN1Primitive.fromByteArray(timestampToken);
                    org.bouncycastle.asn1.cms.Attribute tsAttr = new org.bouncycastle.asn1.cms.Attribute(
                        org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                        new org.bouncycastle.asn1.DERSet(asn1));
                    
                    org.bouncycastle.asn1.cms.AttributeTable unsignedAttrs = 
                        new org.bouncycastle.asn1.cms.AttributeTable(
                            new org.bouncycastle.asn1.DERSet(tsAttr));
                    
                    signer = SignerInformation.replaceUnsignedAttributes(signer, unsignedAttrs);
                    
                    return CMSSignedData.replaceSigners(signedData, 
                        new SignerInformationStore(Collections.singletonList(signer)));
                }
            } catch (Exception e) {
                // Timestamp failed, return original
                LoggerFactory.getLogger(SignatureHandler.class).warn("Timestamp failed: {}", e.getMessage());
            }
            return signedData;
        }
        
        private byte[] getTimestampToken(byte[] signatureHash) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(signatureHash);
                
                org.bouncycastle.tsp.TimeStampRequestGenerator reqGen = 
                    new org.bouncycastle.tsp.TimeStampRequestGenerator();
                reqGen.setCertReq(true);
                
                org.bouncycastle.tsp.TimeStampRequest request = reqGen.generate(
                    org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha256, digest);
                
                byte[] requestBytes = request.getEncoded();
                
                // Send request to TSA
                URL url = new URL(tsaUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/timestamp-query");
                conn.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBytes);
                }
                
                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        org.bouncycastle.tsp.TimeStampResponse response = 
                            new org.bouncycastle.tsp.TimeStampResponse(is);
                        response.validate(request);
                        
                        org.bouncycastle.tsp.TimeStampToken token = response.getTimeStampToken();
                        if (token != null) {
                            return token.getEncoded();
                        }
                    }
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(SignatureHandler.class).warn("TSA request failed: {}", e.getMessage());
            }
            return null;
        }
    }
    
    private static class CMSProcessableInputStream implements CMSTypedData {
        private final InputStream inputStream;
        
        public CMSProcessableInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        
        @Override
        public org.bouncycastle.asn1.ASN1ObjectIdentifier getContentType() {
            return org.bouncycastle.asn1.cms.CMSObjectIdentifiers.data;
        }
        
        @Override
        public Object getContent() {
            return inputStream;
        }
        
        @Override
        public void write(OutputStream out) throws IOException, CMSException {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
    
    // ==================== CONFIGURATION CLASS ====================
    
    public static class SignatureConfig {
        private String keystorePath;
        private String keystorePassword;
        private String keystoreType = "PKCS12";
        private String keyAlias;
        private String keyPassword;
        private String algorithm;
        private String signerName;
        private String reason;
        private String location;
        private boolean visibleSignature = false;
        private int signaturePage = 1;
        private float signatureX = 50;
        private float signatureY = 50;
        private float signatureWidth = 200;
        private float signatureHeight = 50;
        private boolean includeTimestamp = true;
        
        // Getters and setters
        public String getKeystorePath() { return keystorePath; }
        public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }
        public String getKeystorePassword() { return keystorePassword; }
        public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }
        public String getKeystoreType() { return keystoreType; }
        public void setKeystoreType(String keystoreType) { this.keystoreType = keystoreType; }
        public String getKeyAlias() { return keyAlias; }
        public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
        public String getKeyPassword() { return keyPassword; }
        public void setKeyPassword(String keyPassword) { this.keyPassword = keyPassword; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getSignerName() { return signerName; }
        public void setSignerName(String signerName) { this.signerName = signerName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public boolean isVisibleSignature() { return visibleSignature; }
        public void setVisibleSignature(boolean visibleSignature) { this.visibleSignature = visibleSignature; }
        public int getSignaturePage() { return signaturePage; }
        public void setSignaturePage(int signaturePage) { this.signaturePage = signaturePage; }
        public float getSignatureX() { return signatureX; }
        public void setSignatureX(float signatureX) { this.signatureX = signatureX; }
        public float getSignatureY() { return signatureY; }
        public void setSignatureY(float signatureY) { this.signatureY = signatureY; }
        public float getSignatureWidth() { return signatureWidth; }
        public void setSignatureWidth(float signatureWidth) { this.signatureWidth = signatureWidth; }
        public float getSignatureHeight() { return signatureHeight; }
        public void setSignatureHeight(float signatureHeight) { this.signatureHeight = signatureHeight; }
        public boolean isIncludeTimestamp() { return includeTimestamp; }
        public void setIncludeTimestamp(boolean includeTimestamp) { this.includeTimestamp = includeTimestamp; }
    }
}
