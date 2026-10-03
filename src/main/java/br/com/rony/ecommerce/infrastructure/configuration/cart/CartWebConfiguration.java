package br.com.rony.ecommerce.infrastructure.configuration.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CartWebConfiguration {
    @Bean
    public FilterRegistrationBean<CartSessionFilter> cartSessionFilter(
            ObjectMapper mapper,
            @Value("${cart.public-origin:http://localhost:8080}") String publicOrigin,
            @Value("${cart.requests-per-minute:120}") int requestsPerMinute) {
        var registration = new FilterRegistrationBean<>(
                new CartSessionFilter(mapper, publicOrigin, requestsPerMinute));
        registration.addUrlPatterns("/api/carrinho", "/api/carrinho/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
