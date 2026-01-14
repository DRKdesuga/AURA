package com.aura.controller;

import com.aura.dto.ChatResponseDTO;
import com.aura.repository.SessionRepository;
import com.aura.security.CurrentUserProvider;
import com.aura.security.JwtService;
import com.aura.service.ChatService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ChatService chatService;

    @MockBean
    SessionRepository sessionRepository;

    @MockBean
    CurrentUserProvider currentUserProvider;

    @MockBean
    JwtService jwtService;

    @Test
    @DisplayName("POST /api/chat/with-file accepts multipart PDF and returns response")
    void chatWithFile_acceptsMultipartPdf() throws Exception {
        ChatResponseDTO response = ChatResponseDTO.builder()
                .sessionId(10L)
                .userMessageId(1L)
                .assistantMessageId(2L)
                .assistantReply("OK")
                .timestamp(Instant.parse("2025-01-01T00:00:00Z"))
                .newSession(false)
                .build();
        when(chatService.chatWithFile(eq(10L), eq("Hello"), any())).thenReturn(response);

        MockMultipartFile message = new MockMultipartFile(
                "message", "", MediaType.TEXT_PLAIN_VALUE, "Hello".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile sessionId = new MockMultipartFile(
                "sessionId", "", MediaType.TEXT_PLAIN_VALUE, "10".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, samplePdfBytes("Hello"));

        mockMvc.perform(multipart("/api/chat/with-file")
                        .file(message)
                        .file(sessionId)
                        .file(file)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(10))
                .andExpect(jsonPath("$.assistantReply").value("OK"));

        verify(chatService).chatWithFile(eq(10L), eq("Hello"), any());
    }

    private byte[] samplePdfBytes(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
