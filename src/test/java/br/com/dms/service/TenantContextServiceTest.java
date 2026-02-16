package br.com.dms.service;

import br.com.dms.exception.DmsBusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("should resolve tenant from jwt claim when present")
    void shouldResolveTenantFromJwtClaim() {
        TenantContextService service = new TenantContextService(false);
        Jwt jwt = buildJwt(Map.of("tenant_id", "tenant-jwt"));

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        String tenantId = service.requireTenantId("tx-1");

        assertEquals("tenant-jwt", tenantId);
    }

    @Test
    @DisplayName("should fallback to header when enabled and jwt claim missing")
    void shouldFallbackToHeaderWhenEnabled() {
        TenantContextService service = new TenantContextService(true);
        Jwt jwt = buildJwt(Map.of("sub", "user-1"));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-header");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String tenantId = service.requireTenantId("tx-2");

        assertEquals("tenant-header", tenantId);
    }

    @Test
    @DisplayName("should deny when fallback disabled and token has no tenant_id")
    void shouldDenyWhenFallbackDisabledAndMissingTenant() {
        TenantContextService service = new TenantContextService(false);
        Jwt jwt = buildJwt(Map.of("sub", "user-1"));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-header");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThrows(DmsBusinessException.class, () -> service.requireTenantId("tx-3"));
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(300);
        return new Jwt("token", issuedAt, expiresAt, Map.of("alg", "none"), claims);
    }
}
