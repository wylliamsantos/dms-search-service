package br.com.dms.service;

import br.com.dms.domain.mongodb.DmsDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SemanticSearchRankerTest {

    private final SemanticSearchRanker ranker = new SemanticSearchRanker(new HashEmbeddingService());

    @Test
    void shouldPrioritizeSemanticallyCloserDocument() {
        DmsDocument invoice = DmsDocument.of()
            .id("1")
            .filename("nota fiscal fornecedor acme")
            .category("financeiro")
            .build();

        DmsDocument electricity = DmsDocument.of()
            .id("2")
            .filename("fatura de energia eletrica")
            .category("utilidades")
            .build();

        List<DmsDocument> reranked = ranker.rerank(List.of(electricity, invoice), "nota fiscal");

        assertEquals("1", reranked.get(0).getId());
        assertNotNull(reranked.get(0).getContentEmbedding());
    }
}
