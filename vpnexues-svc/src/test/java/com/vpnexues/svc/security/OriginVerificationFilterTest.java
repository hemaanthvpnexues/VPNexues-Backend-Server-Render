package com.vpnexues.svc.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * QA-04 regression: OriginVerificationFilter is the compensating control for
 * disabled CSRF — it must block state-changing requests from foreign origins
 * (form CSRF does not preflight) while letting non-browser clients and the
 * allow-listed app origins straight through.
 */
class OriginVerificationFilterTest {

    private static final String ALLOWED =
            "http://localhost:5173,http://localhost:5174,https://www.vpnexues.com,https://vpnexues.com";

    private OriginVerificationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new OriginVerificationFilter(ALLOWED);
    }

    private MockHttpServletRequest post(String origin, String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setServerName("vpnexues-api.onrender.com");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        if (referer != null) {
            request.addHeader("Referer", referer);
        }
        return request;
    }

    /** Runs the filter; chain.getRequest() != null ⇢ the request passed through. */
    private MockFilterChain run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain;
    }

    @Test
    void safeMethodsAreNeverFiltered() {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/products");
        assertTrue(filter.shouldNotFilter(get), "GET must skip the origin check");
        assertFalse(filter.shouldNotFilter(post("https://evil.example", null)), "POST must be checked");
    }

    @Test
    void passesRequestsWithoutBrowserHeaders() throws Exception {
        // curl / server-to-server / tests send neither Origin nor Referer
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post(null, null), response);
        assertNotNull(chain.getRequest(), "non-browser request must pass");
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsConfiguredAppOrigins() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("https://www.vpnexues.com", null), response);
        assertNotNull(chain.getRequest(), "allow-listed origin must pass");
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsSameHostnameOriginLikeSwaggerUi() throws Exception {
        // Swagger UI served from this API's own host
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("https://vpnexues-api.onrender.com", null), response);
        assertNotNull(chain.getRequest(), "same-host origin must pass");
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowsAllowedOriginViaRefererWhenOriginMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post(null, "https://www.vpnexues.com/gallery"), response);
        assertNotNull(chain.getRequest(), "allow-listed referer must pass");
        assertEquals(200, response.getStatus());
    }

    @Test
    void blocksForeignOrigin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("https://evil.example", null), response);
        assertNull(chain.getRequest(), "evil origin must not reach the chain");
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("blocked"));
    }

    @Test
    void blocksForeignRefererWhenOriginMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post(null, "https://evil.example/attack.html"), response);
        assertNull(chain.getRequest(), "evil referer must not reach the chain");
        assertEquals(403, response.getStatus());
    }

    @Test
    void blocksOpaqueNullOrigin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("null", null), response);
        assertNull(chain.getRequest(), "file:// / sandboxed origin must be rejected");
        assertEquals(403, response.getStatus());
    }

    @Test
    void blocksMalformedOrigin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("not a url", null), response);
        assertNull(chain.getRequest(), "malformed origin must be treated as hostile");
        assertEquals(403, response.getStatus());
    }

    @Test
    void originPortMustMatchAllowListEntry() throws Exception {
        // right host, wrong port → not in the allow-list and not this server
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(post("http://www.vpnexues.com:8443", null), response);
        assertNull(chain.getRequest(), "unlisted host:port must be rejected");
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsGlobPatternAllowListEntries() throws Exception {
        // CORS allows origin PATTERNS (setAllowedOriginPatterns) — the filter must agree
        OriginVerificationFilter globFilter = new OriginVerificationFilter("https://*.vpnexues.com");
        MockHttpServletRequest request = post("https://admin.vpnexues.com", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        globFilter.doFilter(request, response, chain);
        assertNotNull(chain.getRequest(), "glob-matched origin must pass");
        assertEquals(200, response.getStatus());

        MockHttpServletRequest evil = post("https://adminXvpnexues.com", null);
        MockHttpServletResponse evilResponse = new MockHttpServletResponse();
        MockFilterChain evilChain = new MockFilterChain();
        globFilter.doFilter(evil, evilResponse, evilChain);
        assertNull(evilChain.getRequest(), "glob must not over-match");
        assertEquals(403, evilResponse.getStatus());
    }
}
