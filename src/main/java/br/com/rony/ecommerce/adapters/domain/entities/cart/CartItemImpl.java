package br.com.rony.ecommerce.adapters.domain.entities.cart;

import br.com.rony.ecommerce.adapters.domain.entities.product.ProductImpl;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shopping_cart_items", uniqueConstraints =
        @UniqueConstraint(columnNames = {"cart_id", "product_id"}))
public class CartItemImpl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartImpl cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductImpl product;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String photo;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitDiscount;

    @Column(nullable = false)
    private int quantity;

    protected CartItemImpl() {}

    public CartItemImpl(CartImpl cart, ProductImpl product) {
        this.cart = cart;
        this.product = product;
    }

    // Preço, nome e imagem são snapshots; atualização do item renova o snapshot.
    public void update(int quantity, String name, String photo,
                       BigDecimal unitPrice, BigDecimal unitDiscount) {
        this.quantity = quantity;
        this.name = name;
        this.photo = photo;
        this.unitPrice = unitPrice;
        this.unitDiscount = unitDiscount;
    }

    public Long getProductId() { return product.getId(); }
    public String getName() { return name; }
    public String getPhoto() { return photo; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getUnitDiscount() { return unitDiscount; }
    public int getQuantity() { return quantity; }
}
