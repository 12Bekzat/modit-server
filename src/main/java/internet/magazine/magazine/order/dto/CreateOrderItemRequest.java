package internet.magazine.magazine.order.dto;

import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
    @NotNull @Valid ProductSnapshotRequest product,
    @Positive int quantity
) {
}
