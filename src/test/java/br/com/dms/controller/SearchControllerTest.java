package br.com.dms.controller;

import br.com.dms.config.RestTemplateConfig;
import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import br.com.dms.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SearchController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RestTemplateConfig.class))
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false"
})
@Import(SearchControllerTest.TestRestTemplateBuilderConfig.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("should delegate search by author")
    void byAuthorDelegatesToService() throws Exception {
        mockSearchResponse();

        mockMvc.perform(post("/v1/search/byAuthor")
                        .header("TransactionId", "tx-1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("author", "john")
                        .param("skipCount", "1")
                        .param("maxItems", "5")
                        .param("searchScope", SearchScope.ALL.name()))
                .andExpect(status().isOk());

        Mockito.verify(searchService).searchByAuthor(eq("tx-1"), eq("Bearer token"), eq("john"), eq(1), eq(SearchScope.ALL), eq(5));
    }

    @Test
    @DisplayName("should delegate search by metadata")
    void byMetadataDelegatesToService() throws Exception {
        mockSearchResponse();

        mockMvc.perform(post("/v1/search/byMetadata")
                        .header("TransactionId", "tx-2")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("type", "contract")
                        .param("metadata", "{\"foo\":\"bar\"}")
                        .param("versionType", VersionType.MINOR.name()))
                .andExpect(status().isOk());

        Mockito.verify(searchService).searchByMetadata(eq("tx-2"), eq("Bearer token"), eq("contract"), any(), any(), eq("{\"foo\":\"bar\"}"), any(), eq(VersionType.MINOR));
    }

    @Test
    @DisplayName("should delegate search by query")
    void byQueryDelegatesToService() throws Exception {
        mockSearchResponse();

        mockMvc.perform(post("/v1/search/byQuery")
                        .header("TransactionId", "tx-3")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("query", "select")
                        .param("maxItems", "10"))
                .andExpect(status().isOk());

        Mockito.verify(searchService).searchByQuery(eq("tx-3"), eq("Bearer token"), eq("select"), any(), eq(10), eq(VersionType.MAJOR));
    }

    @Test
    @DisplayName("should delegate search by cpf")
    void byCpfDelegatesToService() throws Exception {
        mockSearchResponse();

        String payload = "{\"cpf\":\"123\",\"searchScope\":\"ALL\",\"versionType\":\"MAJOR\",\"documentCategoryNames\":[\"cat\"]}";

        mockMvc.perform(post("/v1/search/byCpf")
                        .header("TransactionId", "tx-4")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(searchService).searchByCpf(eq("tx-4"), eq("Bearer token"), any(SearchByCpfRequest.class));
    }

    private void mockSearchResponse() {
        Page<EntryPagination> page = new PageImpl<>(Collections.emptyList());
        ResponseEntity<Page<EntryPagination>> responseEntity = ResponseEntity.ok(page);

        Mockito.when(searchService.searchByAuthor(any(), any(), any(), any(), any(), any())).thenReturn(responseEntity);
        Mockito.when(searchService.searchByMetadata(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(responseEntity);
        Mockito.when(searchService.searchByQuery(any(), any(), any(), any(), any(), any())).thenReturn(responseEntity);
        Mockito.when(searchService.searchByCpf(any(), any(), ArgumentMatchers.any(SearchByCpfRequest.class))).thenReturn(responseEntity);
    }

    @TestConfiguration
    static class TestRestTemplateBuilderConfig {
        @Bean
        RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder();
        }
    }
}
