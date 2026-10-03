package br.com.rony.ecommerce.adapters.domain.entities.cart;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_carts")
public class CartImpl {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderDiscount = new BigDecimal("0.00");

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartItemImpl> items = new ArrayList<>();

    protected CartImpl() {}

    public CartImpl(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public BigDecimal getOrderDiscount() { return orderDiscount; }
    public List<CartItemImpl> getItems() { return items; }

    public void add(CartItemImpl item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
        orderDiscount = new BigDecimal("0.00");
    }
}
