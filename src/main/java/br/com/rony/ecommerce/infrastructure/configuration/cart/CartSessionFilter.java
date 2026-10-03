package br.com.rony.ecommerce.infrastructure.configuration.cart;

import br.com.rony.ecommerce.application.dto.cart.CartDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class CartSessionFilter extends OncePerRequestFilter {
    public static final String OWNER_ATTRIBUTE = CartSessionFilter.class.getName() + ".owner";
    private static final String STATE_ATTRIBUTE = CartSessionFilter.class.getName() + ".state";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ObjectMapper mapper;
    private final String publicOrigin;
    private final int requestsPerMinute;

    public CartSessionFilter(ObjectMapper mapper, String publicOrigin, int requestsPerMinute) {
        if (requestsPerMinute < 1) {
            throw new IllegalArgumentException("cart.requests-per-minute deve ser positivo");
        }
        this.mapper = mapper;
        this.publicOrigin = publicOrigin;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String origin = request.getHeader("Origin");
        if ((origin != null && !origin.equals(publicOrigin))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
            reject(response, 403, "Origem não autorizada");
            return;
        }
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(204);
            return;
        }

        HttpSession session = request.getSession(true);
        State state;
        synchronized (WebUtils.getSessionMutex(session)) {
            state = (State) session.getAttribute(STATE_ATTRIBUTE);
            if (state == null) {
                state = new State();
                session.setAttribute(STATE_ATTRIBUTE, state);
            }
            long now = System.currentTimeMillis();
            if (now - state.windowStarted >= 60000L || now < state.windowStarted) {
                state.windowStarted = now;
                state.requests = 0;
            }
            if (state.requests >= requestsPerMinute) {
                response.setHeader("Retry-After", "60");
                reject(response, 429, "Limite de requisições excedido. Aguarde um minuto.");
                return;
            }
            state.requests++;
        }

        boolean safe = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (!safe) {
            String token = request.getHeader("X-CSRF-TOKEN");
            if (token == null || token.length() != state.csrf.length()
                    || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                                              state.csrf.getBytes(StandardCharsets.UTF_8))) {
                reject(response, 403, "Token CSRF inválido. Consulte GET /api/carrinho primeiro.");
                return;
            }
        }

        response.setHeader("X-CSRF-TOKEN", state.csrf);
        request.setAttribute(OWNER_ATTRIBUTE, state.owner);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), new CartDTO.Error(false, message, List.of()));
    }

    private static final class State implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String owner = UUID.randomUUID().toString();
        private final String csrf;
        private long windowStarted = System.currentTimeMillis();
        private int requests;

        private State() {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
    }
}
