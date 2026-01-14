package com.aura.service.pdf;

import com.aura.config.PdfChatProperties;
import com.aura.error.AuraErrorCode;
import com.aura.error.AuraException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class PdfTextExtractor {

    private final PdfChatProperties properties;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "pdf-text-extractor");
        thread.setDaemon(true);
        return thread;
    });

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AuraException(AuraErrorCode.INVALID_FILE_TYPE, "Empty file");
        }

        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (maxBytes > 0 && file.getSize() > maxBytes) {
            throw new AuraException(AuraErrorCode.FILE_TOO_LARGE, "File exceeds max size");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("pdf")) {
            throw new AuraException(AuraErrorCode.INVALID_FILE_TYPE, "Only PDF files are supported");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new AuraException(AuraErrorCode.PDF_PARSE_FAILED, "Failed to read PDF");
        }

        if (!hasPdfHeader(data)) {
            throw new AuraException(AuraErrorCode.INVALID_FILE_TYPE, "Invalid PDF header");
        }

        int timeoutSeconds = properties.getParseTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            return clampText(parsePdf(data));
        }

        Future<String> future = executor.submit(() -> parsePdf(data));
        try {
            String text = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return clampText(text);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new AuraException(AuraErrorCode.PDF_PARSE_FAILED, "PDF parsing timed out");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof AuraException auraException) {
                throw auraException;
            }
            throw new AuraException(AuraErrorCode.PDF_PARSE_FAILED, "PDF parsing failed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AuraException(AuraErrorCode.PDF_PARSE_FAILED, "PDF parsing interrupted");
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private String parsePdf(byte[] data) {
        try (PDDocument document = PDDocument.load(data)) {
            if (document.isEncrypted()) {
                throw new AuraException(AuraErrorCode.PDF_ENCRYPTED, "Encrypted PDF is not supported");
            }
            int pages = document.getNumberOfPages();
            if (properties.getMaxPages() > 0 && pages > properties.getMaxPages()) {
                throw new AuraException(AuraErrorCode.PDF_TOO_MANY_PAGES, "PDF has too many pages");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException ex) {
            throw new AuraException(AuraErrorCode.PDF_PARSE_FAILED, "Failed to parse PDF");
        }
    }

    private boolean hasPdfHeader(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        String header = new String(data, 0, 4, StandardCharsets.US_ASCII);
        return header.startsWith("%PDF");
    }

    private String clampText(String text) {
        if (text == null) {
            return "";
        }
        int maxChars = properties.getMaxExtractedChars();
        if (maxChars > 0 && text.length() > maxChars) {
            return text.substring(0, maxChars);
        }
        return text;
    }
}
