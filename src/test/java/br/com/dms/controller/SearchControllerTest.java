package br.com.dms.controller;

import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SearchController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.vault.enabled=false"
})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("should delegate search by cpf")
    void byCpfDelegatesToService() throws Exception {
        String payload = "{\"cpf\":\"123\",\"searchScope\":\"ALL\",\"versionType\":\"MAJOR\",\"documentCategoryNames\":[\"cat\"]}";

        Page<EntryPagination> page = new PageImpl<>(Collections.emptyList());
        ResponseEntity<Page<EntryPagination>> responseEntity = ResponseEntity.ok(page);

        Mockito.when(searchService.searchByCpf(any(), any(), ArgumentMatchers.any(SearchByCpfRequest.class))).thenReturn(responseEntity);

        mockMvc.perform(post("/v1/search/byCpf")
                        .header("TransactionId", "tx-4")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(request -> {
                            request.setUserPrincipal(() -> "admin");
                            return request;
                        }))
                .andExpect(status().isOk());

        Mockito.verify(searchService).searchByCpf(eq("tx-4"), eq("Bearer token"), any(SearchByCpfRequest.class));
    }

    @TestConfiguration
    static class JwtDecoderTestConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return Mockito.mock(JwtDecoder.class);
        }
    }
}
