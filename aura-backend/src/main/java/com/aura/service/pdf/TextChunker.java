package com.aura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<TextChunk> chunk(String text, int chunkSizeChars, int chunkOverlapChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int size = Math.max(1, chunkSizeChars);
        int overlap = Math.max(0, Math.min(chunkOverlapChars, size - 1));

        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + size);
            String slice = text.substring(start, end).trim();
            if (!slice.isEmpty()) {
                chunks.add(new TextChunk(index++, slice));
            }
            if (end == text.length()) {
                break;
            }
            start = end - overlap;
        }
        return chunks;
    }
}
