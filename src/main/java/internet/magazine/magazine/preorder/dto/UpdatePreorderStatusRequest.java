package internet.magazine.magazine.preorder.dto;

import internet.magazine.magazine.preorder.PreorderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePreorderStatusRequest(@NotNull PreorderStatus status) {
}
