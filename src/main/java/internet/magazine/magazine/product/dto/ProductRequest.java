package internet.magazine.magazine.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
    @NotBlank @Size(min = 2, max = 160) String name,
    @NotBlank @Size(min = 2, max = 120) String category,
    @NotBlank @Size(min = 2, max = 120) String brand,
    @NotNull @PositiveOrZero BigDecimal price,
    @NotNull @PositiveOrZero BigDecimal oldPrice,
    @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
    boolean inStock,
    @PositiveOrZero int availableQuantity,
    @NotBlank @Size(min = 2, max = 20) String delivery,
    @NotBlank @Size(min = 2, max = 40) String tag,
    @NotBlank @Size(min = 10, max = 1000) String description,
    @Size(max = 500) String imageUrl,
    @Size(max = 12) List<@Size(max = 500) String> imageUrls,
    @Size(max = 500) String productUrl,
    @Size(max = 32) String currencyCode
) {
}
