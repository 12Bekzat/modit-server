package internet.magazine.magazine.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductSnapshotRequest(
    Long productId,
    @NotBlank @Size(max = 64) String productCode,
    @Size(max = 160) String externalCode,
    @NotBlank @Size(max = 160) String name,
    @Size(max = 120) String category,
    @NotBlank @Size(max = 120) String brand,
    @NotNull @DecimalMin(value = "0.0") BigDecimal price,
    @DecimalMin(value = "0.0") BigDecimal oldPrice,
    @PositiveOrZero int availableQuantity,
    @Size(max = 32) String currencyCode,
    @Size(max = 500) String imageUrl,
    @Size(max = 500) String productUrl,
    @Size(max = 20) String delivery,
    @Size(max = 40) String tag,
    @Size(max = 20) String source
) {
}
