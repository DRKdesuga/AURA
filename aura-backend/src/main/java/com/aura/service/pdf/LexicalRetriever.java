package com.aura.service.pdf;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class LexicalRetriever {

    private static final double DEFAULT_K1 = 1.5;
    private static final double DEFAULT_B = 0.75;

    public List<ScoredChunk> retrieveTopChunks(String query,
                                               List<TextChunk> chunks,
                                               int topK,
                                               double minScoreThreshold) {
        if (query == null || query.isBlank() || chunks == null || chunks.isEmpty() || topK <= 0) {
            return List.of();
        }

        List<List<String>> tokenizedChunks = new ArrayList<>(chunks.size());
        List<Map<String, Integer>> termFrequencies = new ArrayList<>(chunks.size());
        Map<String, Integer> docFrequencies = new HashMap<>();
        int totalTokens = 0;

        for (TextChunk chunk : chunks) {
            List<String> tokens = tokenize(chunk.text());
            tokenizedChunks.add(tokens);
            totalTokens += tokens.size();
            Map<String, Integer> tf = new HashMap<>();
            Set<String> seen = new HashSet<>();
            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
                if (seen.add(token)) {
                    docFrequencies.merge(token, 1, Integer::sum);
                }
            }
            termFrequencies.add(tf);
        }

        double avgDocLength = chunks.isEmpty() ? 0.0 : (double) totalTokens / chunks.size();
        List<String> queryTokens = tokenize(query);

        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Integer> tf = termFrequencies.get(i);
            int docLength = tokenizedChunks.get(i).size();
            double score = bm25Score(queryTokens, tf, docFrequencies, docLength, avgDocLength, chunks.size());
            if (score >= minScoreThreshold) {
                scored.add(new ScoredChunk(chunks.get(i), score));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        if (scored.size() > topK) {
            return scored.subList(0, topK);
        }
        return scored;
    }

    private double bm25Score(List<String> queryTokens,
                             Map<String, Integer> tf,
                             Map<String, Integer> df,
                             int docLength,
                             double avgDocLength,
                             int docCount) {
        if (queryTokens.isEmpty() || docLength == 0 || docCount == 0) {
            return 0.0;
        }
        double score = 0.0;
        for (String token : queryTokens) {
            Integer termFrequency = tf.get(token);
            if (termFrequency == null || termFrequency == 0) {
                continue;
            }
            int docFrequency = df.getOrDefault(token, 0);
            double idf = Math.log(1.0 + (docCount - docFrequency + 0.5) / (docFrequency + 0.5));
            double numerator = termFrequency * (DEFAULT_K1 + 1.0);
            double denominator = termFrequency + DEFAULT_K1 * (1.0 - DEFAULT_B + DEFAULT_B * (docLength / avgDocLength));
            score += idf * (numerator / denominator);
        }
        return score;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
