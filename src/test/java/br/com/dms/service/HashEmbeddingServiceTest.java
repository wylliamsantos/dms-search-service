package br.com.dms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashEmbeddingServiceTest {

    private final HashEmbeddingService service = new HashEmbeddingService();

    @Test
    void shouldGenerateDeterministicEmbedding() {
        double[] first = service.embed("contrato social empresa xyz");
        double[] second = service.embed("contrato social empresa xyz");

        assertEquals(first.length, second.length);
        for (int i = 0; i < first.length; i++) {
            assertEquals(first[i], second[i], 0.0000001);
        }
    }

    @Test
    void shouldReturnHigherSimilarityForCloseTexts() {
        double close = service.cosineSimilarity(
            service.embed("balanco patrimonial 2025"),
            service.embed("balanco patrimonial anual")
        );
        double far = service.cosineSimilarity(
            service.embed("balanco patrimonial 2025"),
            service.embed("comprovante residencia eletricidade")
        );

        assertTrue(close > far);
    }
}
