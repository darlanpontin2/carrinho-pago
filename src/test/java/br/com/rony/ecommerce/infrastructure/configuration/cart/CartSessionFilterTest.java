package br.com.rony.ecommerce.infrastructure.configuration.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class CartSessionFilterTest {
    @Test
    void requiresCsrfAndPreservesSessionOwner() throws Exception {
        var filter = new CartSessionFilter(new ObjectMapper(), "http://localhost:8080", 120);
        var session = new MockHttpSession();
        var get = new MockHttpServletRequest("GET", "/api/carrinho");
        get.setSession(session);
        var getResponse = new MockHttpServletResponse();
        filter.doFilter(get, getResponse, (request, response) -> {});
        String token = getResponse.getHeader("X-CSRF-TOKEN");
        assertNotNull(token);
        Object owner = get.getAttribute(CartSessionFilter.OWNER_ATTRIBUTE);

        var rejected = new MockHttpServletRequest("POST", "/api/carrinho/items");
        rejected.setSession(session);
        var rejectedResponse = new MockHttpServletResponse();
        var reached = new AtomicBoolean();
        filter.doFilter(rejected, rejectedResponse, (request, response) -> reached.set(true));
        assertEquals(403, rejectedResponse.getStatus());
        assertFalse(reached.get());

        var accepted = new MockHttpServletRequest("POST", "/api/carrinho/items");
        accepted.setSession(session);
        accepted.addHeader("X-CSRF-TOKEN", token);
        filter.doFilter(accepted, new MockHttpServletResponse(),
                (request, response) -> reached.set(true));
        assertTrue(reached.get());
        assertEquals(owner, accepted.getAttribute(CartSessionFilter.OWNER_ATTRIBUTE));
    }

    @Test
    void limitsRequestsWithinSession() throws Exception {
        var filter = new CartSessionFilter(new ObjectMapper(), "http://localhost:8080", 1);
        var session = new MockHttpSession();
        var first = new MockHttpServletRequest("GET", "/api/carrinho");
        first.setSession(session);
        filter.doFilter(first, new MockHttpServletResponse(), (request, response) -> {});

        var second = new MockHttpServletRequest("GET", "/api/carrinho");
        second.setSession(session);
        var response = new MockHttpServletResponse();
        filter.doFilter(second, response, (request, result) -> fail("Deveria limitar"));
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void rejectsCrossOriginRequests() throws Exception {
        var filter = new CartSessionFilter(new ObjectMapper(), "http://localhost:8080", 120);
        var request = new MockHttpServletRequest("GET", "/api/carrinho");
        request.addHeader("Origin", "https://outro.example");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Deveria bloquear"));
        assertEquals(403, response.getStatus());
    }
}
