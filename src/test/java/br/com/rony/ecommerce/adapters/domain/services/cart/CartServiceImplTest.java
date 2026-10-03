package br.com.rony.ecommerce.adapters.domain.services.cart;

import br.com.rony.ecommerce.adapters.domain.entities.cart.*;
import br.com.rony.ecommerce.adapters.domain.entities.product.ProductImpl;
import br.com.rony.ecommerce.data.repository.cart.CartRepository;
import br.com.rony.ecommerce.domain.exceptions.CartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceImplTest {
    private CartRepository repository;
    private CartServiceImpl service;
    private CartImpl cart;
    private CartProductOfferImpl offer;

    @BeforeEach
    void setUp() {
        repository = mock(CartRepository.class);
        service = new CartServiceImpl(repository);
        cart = new CartImpl("visitor-a");
        when(repository.lockOrCreate("visitor-a")).thenReturn(cart);

        ProductImpl product = mock(ProductImpl.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Produto A");
        when(product.getPrice()).thenReturn(new BigDecimal("100.00"));
        when(product.getImages()).thenReturn(List.of());
        when(repository.lockProduct(1L)).thenReturn(product);

        offer = mock(CartProductOfferImpl.class);
        when(offer.getStock()).thenReturn(10);
        when(offer.getUnitDiscount()).thenReturn(new BigDecimal("5.00"));
        when(repository.lockOffer(1L)).thenReturn(offer);
    }

    @Test
    void addsWithExactMoneyAndAccumulatesWithoutDuplicates() {
        var first = service.add("visitor-a", 1L, 2).carrinho();
        assertEquals(new BigDecimal("200.00"), first.total());
        assertEquals(new BigDecimal("190.00"), first.discountedTotal());
        assertEquals(new BigDecimal("10.00"), first.items().get(0).discount());

        var second = service.add("visitor-a", 1L, 1).carrinho();
        assertEquals(1, second.items().size());
        assertEquals(3L, second.quantity());
    }

    @Test
    void checksAccumulatedQuantityAgainstStock() {
        service.add("visitor-a", 1L, 8);
        var exception = assertThrows(CartException.class,
                () -> service.add("visitor-a", 1L, 3));
        assertEquals(409, exception.getStatus());
        assertEquals(8, cart.getItems().get(0).getQuantity());
    }

    @Test
    void supportsIncreaseDecreaseRemoveAndEmptyClear() {
        service.add("visitor-a", 1L, 2);
        assertEquals(4L, service.update("visitor-a", 1L, 4).carrinho().quantity());
        assertEquals(1L, service.update("visitor-a", 1L, 1).carrinho().quantity());
        assertTrue(service.remove("visitor-a", 1L).carrinho().items().isEmpty());
        assertEquals(new BigDecimal("0.00"), service.clear("visitor-a").carrinho().total());
    }

    @Test
    void rejectsInvalidQuantityMissingProductAndMissingItem() {
        assertEquals(400, assertThrows(CartException.class,
                () -> service.add("visitor-a", 1L, 0)).getStatus());
        assertEquals(404, assertThrows(CartException.class,
                () -> service.add("visitor-a", 99L, 1)).getStatus());
        assertEquals(404, assertThrows(CartException.class,
                () -> service.update("visitor-a", 1L, 1)).getStatus());
    }

    @Test
    void isolatesVisitors() {
        when(repository.lockOrCreate("visitor-b")).thenReturn(new CartImpl("visitor-b"));
        service.add("visitor-a", 1L, 2);
        assertTrue(service.get("visitor-b").carrinho().items().isEmpty());
        assertEquals(404, assertThrows(CartException.class,
                () -> service.remove("visitor-b", 1L)).getStatus());
        assertEquals(2L, service.get("visitor-a").carrinho().quantity());
    }

    @Test
    void rejectsDiscountGreaterThanPrice() {
        when(offer.getUnitDiscount()).thenReturn(new BigDecimal("101.00"));
        assertEquals(409, assertThrows(CartException.class,
                () -> service.add("visitor-a", 1L, 1)).getStatus());
        assertTrue(cart.getItems().isEmpty());
    }
}
