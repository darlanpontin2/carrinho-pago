CREATE TABLE cart_product_offers (
    product_id BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    unit_discount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT cart_offer_pk PRIMARY KEY (product_id),
    CONSTRAINT cart_offer_product_fk FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT cart_offer_stock_ck CHECK (stock >= 0),
    CONSTRAINT cart_offer_discount_ck CHECK (unit_discount >= 0)
) ENGINE=InnoDB;

INSERT INTO cart_product_offers (product_id, stock, unit_discount)
SELECT id, 0, 0.00 FROM products;

CREATE TABLE shopping_carts (
    id VARCHAR(36) NOT NULL,
    order_discount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT shopping_cart_pk PRIMARY KEY (id),
    CONSTRAINT shopping_cart_discount_ck CHECK (order_discount >= 0)
) ENGINE=InnoDB;

CREATE TABLE shopping_cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id VARCHAR(36) NOT NULL,
    product_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    photo VARCHAR(255),
    unit_price DECIMAL(19,2) NOT NULL,
    unit_discount DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL,
    CONSTRAINT shopping_cart_item_pk PRIMARY KEY (id),
    CONSTRAINT shopping_cart_item_unique UNIQUE (cart_id, product_id),
    CONSTRAINT shopping_cart_item_cart_fk FOREIGN KEY (cart_id)
        REFERENCES shopping_carts (id) ON DELETE CASCADE,
    CONSTRAINT shopping_cart_item_product_fk FOREIGN KEY (product_id)
        REFERENCES products (id),
    CONSTRAINT shopping_cart_item_quantity_ck CHECK (quantity BETWEEN 1 AND 1000000),
    CONSTRAINT shopping_cart_item_price_ck CHECK (unit_price >= 0),
    CONSTRAINT shopping_cart_item_discount_ck
        CHECK (unit_discount >= 0 AND unit_discount <= unit_price),
    INDEX shopping_cart_item_product_idx (product_id)
) ENGINE=InnoDB;
