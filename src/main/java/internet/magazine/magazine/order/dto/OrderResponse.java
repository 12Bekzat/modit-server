package internet.magazine.magazine.order.dto;

import internet.magazine.magazine.order.OrderEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String firstName,
    String lastName,
    String phone,
    String email,
    String comment,
    String status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt,
    String userEmail
) {

    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
            order.getId(),
            order.getFirstName(),
            order.getLastName(),
            order.getPhone(),
            order.getEmail(),
            order.getComment(),
            order.getStatus().name(),
            order.getTotalAmount(),
            order.getItems().stream().map(OrderItemResponse::from).toList(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getUser() != null ? order.getUser().getEmail() : null
        );
    }
}
