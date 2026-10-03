package br.com.rony.ecommerce.application.controllers.cart;

import br.com.rony.ecommerce.application.dto.cart.CartDTO;
import br.com.rony.ecommerce.domain.services.cart.CartService;
import br.com.rony.ecommerce.infrastructure.configuration.cart.CartSessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/carrinho")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartDTO.Response get(HttpServletRequest request) {
        return service.get(owner(request));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO.Response> add(HttpServletRequest request,
                                               @Valid @RequestBody CartDTO.AddItem form) {
        return ResponseEntity.created(URI.create("/api/carrinho"))
                .body(service.add(owner(request), form.productId(), form.quantity()));
    }

    // A quantidade é absoluta: aceita tanto aumento quanto diminuição.
    @RequestMapping(value = "/items/{productId}/quantidade",
            method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CartDTO.Response update(HttpServletRequest request,
                                   @PathVariable("productId") Long productId,
                                   @Valid @RequestBody CartDTO.UpdateQuantity form) {
        return service.update(owner(request), productId, form.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartDTO.Response remove(HttpServletRequest request,
                                   @PathVariable("productId") Long productId) {
        return service.remove(owner(request), productId);
    }

    @DeleteMapping("/clear")
    public CartDTO.Response clear(HttpServletRequest request) {
        return service.clear(owner(request));
    }

    private String owner(HttpServletRequest request) {
        // Nunca aceita proprietário por header, path, query string ou JSON.
        return (String) request.getAttribute(CartSessionFilter.OWNER_ATTRIBUTE);
    }
}
