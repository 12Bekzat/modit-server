package internet.magazine.magazine.order.dto;

import internet.magazine.magazine.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
