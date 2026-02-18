package br.com.dms.controller;

import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setupSearchService() {
        Page<EntryPagination> page = new PageImpl<>(Collections.emptyList());
        ResponseEntity<Page<EntryPagination>> responseEntity = ResponseEntity.ok(page);
        Mockito.when(searchService.searchByCpf(any(), any(), ArgumentMatchers.any(SearchByCpfRequest.class)))
                .thenReturn(responseEntity);
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "admin", "reviewer", "viewer", "document_viewer", "OWNER", "ADMIN", "REVIEWER", "VIEWER", "DOCUMENT_VIEWER"})
    @DisplayName("should allow access for production roles (lower/upper case)")
    void byCpfAllowsProductionRoles(String role) throws Exception {
        Mockito.when(jwtDecoder.decode(eq("token"))).thenReturn(jwtWithRoles(role));

        mockMvc.perform(post("/v1/search/byCpf")
                        .header("TransactionId", "tx-4")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload())
                        .with(request -> {
                            request.setUserPrincipal(() -> "user");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should deny access for non-authorized role")
    void byCpfDeniesUnauthorizedRole() throws Exception {
        Mockito.when(jwtDecoder.decode(eq("token"))).thenReturn(jwtWithRoles("GUEST"));

        mockMvc.perform(post("/v1/search/byCpf")
                        .header("TransactionId", "tx-4")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());

        Mockito.verify(searchService, Mockito.never()).searchByCpf(any(), any(), any(SearchByCpfRequest.class));
    }

    @Test
    @DisplayName("should require authentication")
    void byCpfRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/v1/search/byCpf")
                        .header("TransactionId", "tx-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    private static String validPayload() {
        return "{\"cpf\":\"123\",\"searchScope\":\"ALL\",\"versionType\":\"MAJOR\",\"documentCategoryNames\":[\"cat\"]}";
    }

    private static Jwt jwtWithRoles(String... roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(60);
        Map<String, Object> headers = Map.of("alg", "none");
        Map<String, Object> claims = Map.of(
                "realm_access", Map.of("roles", List.of(roles))
        );

        return new Jwt("token", issuedAt, expiresAt, headers, claims);
    }
}
