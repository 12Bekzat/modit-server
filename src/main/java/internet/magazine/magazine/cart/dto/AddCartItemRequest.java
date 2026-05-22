package internet.magazine.magazine.cart.dto;

import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
    @NotNull @Valid ProductSnapshotRequest product,
    @Positive int quantity
) {
}
