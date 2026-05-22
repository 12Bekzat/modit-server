package internet.magazine.magazine.cart.dto;

import internet.magazine.magazine.cart.CartItem;
import java.math.BigDecimal;

public record CartItemResponse(
    String productCode,
    String productExternalCode,
    String name,
    String category,
    String brand,
    BigDecimal price,
    BigDecimal oldPrice,
    String currencyCode,
    String imageUrl,
    String productUrl,
    String delivery,
    String tag,
    String source,
    int availableQuantity,
    int quantity,
    BigDecimal lineTotal
) {

    public static CartItemResponse from(CartItem item) {
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        return new CartItemResponse(
            item.getProductCode(),
            item.getProductExternalCode(),
            item.getProductName(),
            item.getProductCategory(),
            item.getProductBrand(),
            price,
            item.getOldPrice() != null ? item.getOldPrice() : price,
            item.getCurrencyCode(),
            item.getImageUrl(),
            item.getProductUrl(),
            item.getDelivery(),
            item.getTag(),
            item.getSource(),
            item.getAvailableQuantity(),
            item.getQuantity(),
            price.multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
