package internet.magazine.magazine.product.dto;

import internet.magazine.magazine.product.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
    Long id,
    String productCode,
    String name,
    String category,
    String brand,
    BigDecimal price,
    BigDecimal oldPrice,
    BigDecimal rating,
    boolean inStock,
    int availableQuantity,
    String delivery,
    String tag,
    String description,
    String source,
    String externalCode,
    String currencyCode,
    String imageUrl,
    List<String> imageUrls,
    String productUrl,
    Instant createdAt,
    Instant updatedAt,
    Instant lastSyncedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getProductCode(),
            product.getName(),
            product.getCategory(),
            product.getBrand(),
            product.getPrice(),
            product.getOldPrice(),
            product.getRating(),
            product.isInStock(),
            product.getAvailableQuantity(),
            product.getDelivery(),
            product.getTag(),
            product.getDescription(),
            product.getSource().name(),
            product.getExternalCode(),
            product.getCurrencyCode(),
            product.getImageUrl(),
            product.getImageUrls(),
            product.getProductUrl(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getLastSyncedAt()
        );
    }
}
