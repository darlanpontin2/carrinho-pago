package br.com.rony.ecommerce.adapters.domain.services.cart;

import br.com.rony.ecommerce.adapters.domain.entities.cart.*;
import br.com.rony.ecommerce.adapters.domain.entities.product.Image;
import br.com.rony.ecommerce.application.dto.cart.CartDTO;
import br.com.rony.ecommerce.data.repository.cart.CartRepository;
import br.com.rony.ecommerce.domain.exceptions.CartException;
import br.com.rony.ecommerce.domain.services.cart.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final CartRepository repository;

    public CartServiceImpl(CartRepository repository) {
        this.repository = repository;
    }

    @Override
    public CartDTO.Response get(String owner) {
        return response(repository.lockOrCreate(owner), "Carrinho recuperado com sucesso");
    }

    @Override
    public CartDTO.Response add(String owner, Long productId, Integer quantity) {
        validate(productId, quantity);
        CartImpl cart = repository.lockOrCreate(owner);
        CartItemImpl item = find(cart, productId);
        long desired = (item == null ? 0L : item.getQuantity()) + quantity;
        if (desired > 1000000) {
            throw new CartException(400, "Quantidade máxima por produto: 1000000");
        }
        if (item == null && cart.getItems().size() >= 100) {
            throw new CartException(409, "Limite de 100 produtos distintos no carrinho");
        }
        item = updateSnapshot(cart, item, productId, (int) desired);
        repository.flush();
        return response(cart, "Item '" + item.getName() + "' adicionado com sucesso no carrinho");
    }

    @Override
    public CartDTO.Response update(String owner, Long productId, Integer quantity) {
        validate(productId, quantity);
        CartImpl cart = repository.lockOrCreate(owner);
        CartItemImpl item = requireItem(cart, productId);
        updateSnapshot(cart, item, productId, quantity);
        repository.flush();
        return response(cart, "Quantidade do item '" + item.getName() + "' atualizada para " + quantity);
    }

    @Override
    public CartDTO.Response remove(String owner, Long productId) {
        validate(productId, 1);
        CartImpl cart = repository.lockOrCreate(owner);
        CartItemImpl item = requireItem(cart, productId);
        cart.getItems().remove(item);
        if (cart.getItems().isEmpty()) {
            cart.clear();
        }
        repository.flush();
        return response(cart, "Item '" + item.getName() + "' removido do carrinho");
    }

    @Override
    public CartDTO.Response clear(String owner) {
        CartImpl cart = repository.lockOrCreate(owner);
        cart.clear();
        repository.flush();
        return response(cart, "Todos os itens foram removidos do carrinho");
    }

    private CartItemImpl updateSnapshot(CartImpl cart, CartItemImpl item,
                                         Long productId, int quantity) {
        var product = repository.lockProduct(productId);
        if (product == null) {
            throw new CartException(404, "Produto não encontrado no catálogo");
        }
        var offer = repository.lockOffer(productId);
        if (offer == null) {
            throw new CartException(409, "Estoque do produto ainda não configurado");
        }
        if (quantity > offer.getStock()) {
            throw new CartException(409, "Estoque insuficiente. Disponível: " + offer.getStock());
        }
        BigDecimal price = money(product.getPrice());
        BigDecimal discount = money(offer.getUnitDiscount());
        if (discount.compareTo(price) > 0) {
            throw new CartException(409, "Desconto do produto superior ao preço");
        }
        String photo = product.getImages() == null ? null : product.getImages().stream()
                .min(Comparator.comparing(Image::getImageOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Image::getKey,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Image::getUrl).orElse(null);
        if (item == null) {
            item = new CartItemImpl(cart, product);
            cart.add(item);
        }
        item.update(quantity, product.getName(), photo, price, discount);
        return item;
    }

    private CartItemImpl find(CartImpl cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst().orElse(null);
    }

    private CartItemImpl requireItem(CartImpl cart, Long productId) {
        CartItemImpl item = find(cart, productId);
        if (item == null) {
            throw new CartException(404, "Item não encontrado no carrinho");
        }
        return item;
    }

    private void validate(Long productId, Integer quantity) {
        if (productId == null || productId <= 0) {
            throw new CartException(400, "produto_id deve ser um inteiro positivo");
        }
        if (quantity == null || quantity <= 0 || quantity > 1000000) {
            throw new CartException(400, "quantidade deve ser um inteiro entre 1 e 1000000");
        }
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new CartException(409, "Configuração monetária inválida");
        }
        try {
            // Não aceita arredondamento silencioso de preço configurado incorretamente.
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new CartException(409, "Valores monetários devem ter no máximo duas casas decimais");
        }
    }

    private CartDTO.Response response(CartImpl cart, String message) {
        List<CartDTO.Item> items = cart.getItems().stream().map(item -> {
            BigDecimal total = money(item.getUnitPrice())
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal discount = money(item.getUnitDiscount())
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartDTO.Item(item.getProductId(), item.getName(), item.getPhoto(),
                    item.getUnitPrice(), item.getQuantity(), total, discount,
                    total.subtract(discount));
        }).toList();
        BigDecimal total = items.stream().map(CartDTO.Item::total).reduce(ZERO, BigDecimal::add);
        BigDecimal subtotal = items.stream().map(CartDTO.Item::discountedTotal)
                .reduce(ZERO, BigDecimal::add);
        // Desconto geral é um valor fixo, aplicado depois dos descontos por unidade.
        BigDecimal orderDiscount = money(cart.getOrderDiscount()).min(subtotal);
        long quantity = items.stream().mapToLong(CartDTO.Item::quantity).sum();
        return new CartDTO.Response(true, message, new CartDTO.Cart(cart.getId(), items,
                total, orderDiscount, subtotal.subtract(orderDiscount), quantity));
    }
}
