package br.com.rony.ecommerce.adapters.domain.entities.cart;

import br.com.rony.ecommerce.adapters.domain.entities.product.ProductImpl;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_product_offers")
public class CartProductOfferImpl {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private ProductImpl product;

    @Column(nullable = false)
    private int stock;

    @Column(name = "unit_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitDiscount;

    protected CartProductOfferImpl() {}

    public int getStock() { return stock; }
    public BigDecimal getUnitDiscount() { return unitDiscount; }
}
