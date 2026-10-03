package br.com.rony.ecommerce.domain.services.cart;

import br.com.rony.ecommerce.application.dto.cart.CartDTO;

public interface CartService {
    CartDTO.Response get(String owner);
    CartDTO.Response add(String owner, Long productId, Integer quantity);
    CartDTO.Response update(String owner, Long productId, Integer quantity);
    CartDTO.Response remove(String owner, Long productId);
    CartDTO.Response clear(String owner);
}
