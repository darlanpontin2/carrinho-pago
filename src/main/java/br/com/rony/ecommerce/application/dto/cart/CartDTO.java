package br.com.rony.ecommerce.application.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public final class CartDTO {
    private CartDTO() {}

    public record AddItem(
            @JsonProperty("produto_id") @NotNull @Positive
            @JsonDeserialize(using = StrictLong.class) Long productId,
            @JsonProperty("quantidade") @NotNull @Min(1) @Max(1000000)
            @JsonDeserialize(using = StrictInteger.class) Integer quantity) {}

    public record UpdateQuantity(
            @JsonProperty("quantidade") @NotNull @Min(1) @Max(1000000)
            @JsonDeserialize(using = StrictInteger.class) Integer quantity) {}

    public record Item(
            @JsonProperty("produto_id") Long productId,
            @JsonProperty("nome") String name,
            @JsonProperty("foto") String photo,
            @JsonProperty("valor_unitario") BigDecimal unitPrice,
            @JsonProperty("quantidade") int quantity,
            @JsonProperty("valor_total") BigDecimal total,
            @JsonProperty("desconto") BigDecimal discount,
            @JsonProperty("valor_com_desconto") BigDecimal discountedTotal) {}

    public record Cart(
            @JsonProperty("id_carrinho") String id,
            @JsonProperty("itens") List<Item> items,
            @JsonProperty("valor_total_carrinho") BigDecimal total,
            @JsonProperty("desconto_geral") BigDecimal orderDiscount,
            @JsonProperty("valor_total_com_desconto") BigDecimal discountedTotal,
            @JsonProperty("quantidade_total_itens") long quantity) {
        // Compatibilidade com os dois nomes presentes nos exemplos solicitados.
        @JsonProperty("quantidade_itens")
        public long quantityAlias() { return quantity; }
    }

    public record Response(boolean sucesso, String mensagem, Cart carrinho) {}

    public record Error(boolean sucesso, String mensagem, List<String> dados) {}

    public static class StrictInteger extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return (Integer) context.handleUnexpectedToken(Integer.class, parser);
            }
            return parser.getIntValue();
        }
    }

    public static class StrictLong extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
            return parser.getLongValue();
        }
    }
}
