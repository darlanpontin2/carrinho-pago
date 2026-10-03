package br.com.rony.ecommerce.data.repository.cart;

import br.com.rony.ecommerce.adapters.domain.entities.cart.*;
import br.com.rony.ecommerce.adapters.domain.entities.product.ProductImpl;

public interface CartRepository {
    CartImpl lockOrCreate(String owner);
    ProductImpl lockProduct(Long productId);
    CartProductOfferImpl lockOffer(Long productId);
    void flush();
}
