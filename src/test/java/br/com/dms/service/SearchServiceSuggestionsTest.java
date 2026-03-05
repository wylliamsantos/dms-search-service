package br.com.dms.service;

import br.com.dms.audit.AuditActorResolver;
import br.com.dms.audit.AuditEventPublisher;
import br.com.dms.domain.mongodb.DmsDocument;
import br.com.dms.repository.mongo.DocumentCategoryRepository;
import br.com.dms.repository.mongo.DmsDocumentRepository;
import br.com.dms.repository.mongo.DmsDocumentVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceSuggestionsTest {

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
    @DisplayName("should fetch a bounded page and return sorted suggestions")
    void shouldFetchBoundedPageAndSortSuggestions() {
        when(tenantContextService.requireTenantId("tx-1")).thenReturn("tenant-1");

        DmsDocument first = new DmsDocument();
        first.setCategory("Contrato");
        first.setFilename("Contrato de aluguel");
        first.setCpf("12345678900");

        DmsDocument second = new DmsDocument();
        second.setCategory("Cobrança");
        second.setFilename("Conta de consumo");

        when(dmsDocumentRepository.findByTenantId(eq("tenant-1"), any(Pageable.class)))
            .thenReturn(List.of(first, second));

        ResponseEntity<List<String>> response = searchService.suggestions("tx-1", "con", null, 5);

        assertThat(response.getBody())
            .containsExactly("Contrato", "Conta de consumo", "Contrato de aluguel");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dmsDocumentRepository).findByTenantId(eq("tenant-1"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(120);
    }

    @Test
    @DisplayName("should use category-filtered query when categories are provided")
    void shouldUseCategoryFilteredQuery() {
        when(tenantContextService.requireTenantId("tx-2")).thenReturn("tenant-2");
        when(dmsDocumentRepository.findByTenantIdAndCategoryIn(eq("tenant-2"), anyList(), any(Pageable.class)))
            .thenReturn(List.of());

        searchService.suggestions("tx-2", "cont", List.of("Financeiro"), 10);

        verify(dmsDocumentRepository).findByTenantIdAndCategoryIn(eq("tenant-2"), eq(List.of("Financeiro")), any(Pageable.class));
        verify(dmsDocumentRepository, never()).findByTenantId(eq("tenant-2"), any(Pageable.class));
    }
}
