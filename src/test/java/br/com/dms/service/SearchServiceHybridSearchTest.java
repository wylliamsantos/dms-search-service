package br.com.dms.service;

import br.com.dms.audit.AuditActorResolver;
import br.com.dms.audit.AuditEventPublisher;
import br.com.dms.controller.request.SearchByBusinessKeyRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.domain.core.VersionType;
import br.com.dms.domain.mongodb.DocumentCategory;
import br.com.dms.domain.mongodb.DmsDocument;
import br.com.dms.domain.mongodb.DmsDocumentVersion;
import br.com.dms.repository.mongo.DocumentCategoryRepository;
import br.com.dms.repository.mongo.DmsDocumentRepository;
import br.com.dms.repository.mongo.DmsDocumentVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceHybridSearchTest {

    @Mock
    private Environment environment;
    @Mock
    private DocumentCategoryRepository documentCategoryRepository;
    @Mock
    private DmsDocumentRepository dmsDocumentRepository;
    @Mock
    private DmsDocumentVersionRepository dmsDocumentVersionRepository;
    @Mock
    private TenantContextService tenantContextService;
    @Mock
    private AuditEventPublisher auditEventPublisher;
    @Mock
    private AuditActorResolver auditActorResolver;
    @Mock
    private SemanticSearchRanker semanticSearchRanker;

    @InjectMocks
    private SearchService searchService;

    @Test
    @DisplayName("should apply hybrid reranking order before pagination")
    void shouldApplyHybridRerankingOrderBeforePagination() {
        when(tenantContextService.requireTenantId("tx-hybrid")).thenReturn("tenant-1");

        DocumentCategory category = new DocumentCategory();
        category.setTenantId("tenant-1");
        category.setName("Financeiro");
        when(documentCategoryRepository.findByTenantId("tenant-1")).thenReturn(Optional.of(List.of(category)));

        DmsDocument docA = DmsDocument.of()
            .id("doc-a")
            .tenantId("tenant-1")
            .filename("Nota fiscal ACME")
            .category("Financeiro")
            .cpf("12345678900")
            .build();

        DmsDocument docB = DmsDocument.of()
            .id("doc-b")
            .tenantId("tenant-1")
            .filename("Comprovante interno")
            .category("Financeiro")
            .cpf("12345678900")
            .build();

        when(dmsDocumentRepository.searchByTenantCategoryAndText(eq("tenant-1"), eq(List.of("Financeiro")), eq("nota"), any(Sort.class)))
            .thenReturn(List.of(docB, docA));
        when(semanticSearchRanker.rerank(List.of(docB, docA), "nota"))
            .thenReturn(List.of(docA, docB));

        when(dmsDocumentVersionRepository.findByTenantIdAndDmsDocumentId("tenant-1", "doc-a"))
            .thenReturn(Optional.of(List.of(version("tenant-1", "doc-a"))));
        when(dmsDocumentVersionRepository.findByTenantIdAndDmsDocumentId("tenant-1", "doc-b"))
            .thenReturn(Optional.of(List.of(version("tenant-1", "doc-b"))));

        SearchByBusinessKeyRequest request = new SearchByBusinessKeyRequest();
        request.setBusinessKeyType("cpf");
        request.setBusinessKeyValue("12345678900");
        request.setDocumentCategoryNames(List.of("Financeiro"));
        request.setVersionType(VersionType.MAJOR);
        request.setTextQuery("nota");
        request.setPage(0);
        request.setSize(10);

        ResponseEntity<Page<EntryPagination>> response = searchService.searchByBusinessKey("tx-hybrid", "Bearer token", request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).extracting(EntryPagination::getId)
            .containsExactly("doc-a");

        verify(semanticSearchRanker).rerank(List.of(docB, docA), "nota");
    }

    private DmsDocumentVersion version(String tenantId, String documentId) {
        return DmsDocumentVersion.of()
            .id("v-" + documentId)
            .tenantId(tenantId)
            .dmsDocumentId(documentId)
            .versionType(VersionType.MAJOR)
            .versionNumber(BigDecimal.ONE)
            .fileSize(1024L)
            .creationDate(LocalDateTime.now())
            .modifiedAt(LocalDateTime.now())
            .build();
    }
}
