package internet.magazine.magazine.order.dto;

import internet.magazine.magazine.order.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
    String productName,
    String productCode,
    String productExternalCode,
    String productBrand,
    String imageUrl,
    BigDecimal price,
    int quantity,
    BigDecimal lineTotal
) {

    public static OrderItemResponse from(OrderItem item) {
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
        return new OrderItemResponse(
            item.getProductName(),
            item.getProductCode(),
            item.getProductExternalCode(),
            item.getProductBrand(),
            item.getImageUrl(),
            price,
            item.getQuantity(),
            price.multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
