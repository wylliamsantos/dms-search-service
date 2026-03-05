package br.com.dms.service;

import br.com.dms.domain.mongodb.DmsDocument;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SemanticSearchRanker {

    private static final double KEYWORD_WEIGHT = 0.65d;
    private static final double SEMANTIC_WEIGHT = 0.35d;

    private final HashEmbeddingService hashEmbeddingService;

    public SemanticSearchRanker(HashEmbeddingService hashEmbeddingService) {
        this.hashEmbeddingService = hashEmbeddingService;
    }

    public List<DmsDocument> rerank(List<DmsDocument> documents, String query) {
        if (StringUtils.isBlank(query) || documents == null || documents.size() < 2) {
            return documents;
        }

        double[] queryEmbedding = hashEmbeddingService.embed(query);

        List<ScoredDocument> scored = new ArrayList<>();
        for (DmsDocument document : documents) {
            String searchable = buildSearchableText(document);
            double keywordScore = keywordScore(searchable, query);
            double[] documentEmbedding = resolveDocumentEmbedding(document, searchable);
            double semanticScore = hashEmbeddingService.cosineSimilarity(queryEmbedding, documentEmbedding);
            double hybridScore = (keywordScore * KEYWORD_WEIGHT) + (semanticScore * SEMANTIC_WEIGHT);
            scored.add(new ScoredDocument(document, hybridScore));
        }

        return scored.stream()
            .sorted(Comparator.comparing(ScoredDocument::score).reversed())
            .map(ScoredDocument::document)
            .toList();
    }

    private double[] resolveDocumentEmbedding(DmsDocument document, String searchable) {
        if (document.getContentEmbedding() != null && !document.getContentEmbedding().isEmpty()) {
            double[] stored = new double[document.getContentEmbedding().size()];
            for (int i = 0; i < document.getContentEmbedding().size(); i++) {
                Double value = document.getContentEmbedding().get(i);
                stored[i] = value == null ? 0.0d : value;
            }
            return stored;
        }

        double[] generated = hashEmbeddingService.embed(searchable);
        document.setContentEmbedding(toList(generated));
        return generated;
    }

    private java.util.List<Double> toList(double[] values) {
        java.util.List<Double> list = new java.util.ArrayList<>(values.length);
        for (double value : values) {
            list.add(value);
        }
        return list;
    }

    private double keywordScore(String searchable, String query) {
        String source = StringUtils.defaultString(searchable).toLowerCase(Locale.ROOT);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        if (source.isBlank()) {
            return 0.0d;
        }
        if (source.equals(normalizedQuery)) {
            return 1.0d;
        }
        if (source.contains(normalizedQuery)) {
            return 0.85d;
        }

        String[] terms = normalizedQuery.split("\\W+");
        if (terms.length == 0) {
            return 0.0d;
        }

        int matched = 0;
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            if (source.contains(term)) {
                matched++;
            }
        }
        return Math.min(0.8d, (double) matched / (double) terms.length);
    }

    private String buildSearchableText(DmsDocument document) {
        StringBuilder builder = new StringBuilder();
        append(builder, document.getFilename());
        append(builder, document.getCategory());
        append(builder, document.getCpf());
        append(builder, document.getOcrText());
        append(builder, flatten(document.getMetadata()));
        return builder.toString();
    }

    private String flatten(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        return metadata.values().stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.joining(" "));
    }

    private void append(StringBuilder builder, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value);
    }

    private record ScoredDocument(DmsDocument document, double score) {
    }
}
