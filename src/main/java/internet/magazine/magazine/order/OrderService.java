package internet.magazine.magazine.order;

import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.order.dto.CreateOrderItemRequest;
import internet.magazine.magazine.order.dto.CreateOrderRequest;
import internet.magazine.magazine.order.dto.OrderResponse;
import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(
        OrderRepository orderRepository,
        UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String userEmail) {
        OrderEntity order = new OrderEntity();
        order.setFirstName(request.firstName().trim());
        order.setLastName(blankToNull(request.lastName()));
        order.setPhone(request.phone().trim());
        order.setEmail(blankToNull(request.email()));
        order.setComment(buildComment(request));
        order.setStatus(OrderStatus.NEW);

        if (userEmail != null) {
            UserAccount user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
            order.setUser(user);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.items()) {
            ProductSnapshotRequest product = itemRequest.product();
            validateAvailability(product, itemRequest.quantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductName(product.name().trim());
            item.setProductCode(product.productCode().trim());
            item.setProductExternalCode(blankToNull(product.externalCode()));
            item.setProductBrand(blankToNull(product.brand()));
            item.setImageUrl(blankToNull(product.imageUrl()));
            item.setPrice(product.price());
            item.setQuantity(itemRequest.quantity());
            order.getItems().add(item);

            totalAmount = totalAmount.add(product.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(totalAmount);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderResponse::from)
            .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        OrderEntity order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order with id '%s' was not found.".formatted(id)));
        order.setStatus(status);
        return OrderResponse.from(orderRepository.save(order));
    }

    private void validateAvailability(ProductSnapshotRequest product, int quantity) {
        if (product.availableQuantity() <= 0) {
            throw new IllegalStateException("Product '%s' is not available for order.".formatted(product.name()));
        }
        if (quantity > product.availableQuantity()) {
            throw new IllegalStateException(
                "Requested quantity for '%s' exceeds available stock (%s).".formatted(
                    product.name(),
                    product.availableQuantity()
                )
            );
        }
    }

    private String buildComment(CreateOrderRequest request) {
        String baseComment = blankToNull(request.comment());
        String orderItems = request.items().stream()
            .map(item -> "[productCode=%s, qty=%s]".formatted(item.product().productCode(), item.quantity()))
            .reduce((left, right) -> left + " | " + right)
            .orElse("");

        if (baseComment == null) {
            return orderItems;
        }

        return orderItems.isBlank() ? baseComment : baseComment + System.lineSeparator() + orderItems;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
