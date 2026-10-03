package br.com.rony.ecommerce.adapters.data.repository.cart;

import br.com.rony.ecommerce.adapters.domain.entities.cart.*;
import br.com.rony.ecommerce.adapters.domain.entities.product.ProductImpl;
import br.com.rony.ecommerce.data.repository.cart.CartRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepositoryImpl implements CartRepository {
    private final EntityManager entityManager;

    public CartRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CartImpl lockOrCreate(String owner) {
        // Evita a corrida "consultar inexistente -> inserir duas vezes".
        // O lock do carrinho serializa inclusive adições do mesmo produto.
        entityManager.createNativeQuery("""
                INSERT INTO shopping_carts (id, order_discount)
                VALUES (:id, 0.00)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter("id", owner).executeUpdate();
        return entityManager.find(CartImpl.class, owner, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public ProductImpl lockProduct(Long productId) {
        return entityManager.find(ProductImpl.class, productId, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public CartProductOfferImpl lockOffer(Long productId) {
        return entityManager.find(CartProductOfferImpl.class, productId,
                LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }
}
