package internet.magazine.magazine.cart;

import internet.magazine.magazine.cart.dto.AddCartItemRequest;
import internet.magazine.magazine.cart.dto.CartItemResponse;
import internet.magazine.magazine.cart.dto.CartResponse;
import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.product.ProductRepository;
import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
        CartRepository cartRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse findCurrentCart(String userEmail) {
        UserAccount user = getUser(userEmail);
        return buildResponse(cartRepository.findAllByUserId(user.getId()));
    }

    @Transactional
    public CartResponse addItem(String userEmail, AddCartItemRequest request) {
        UserAccount user = getUser(userEmail);
        ProductSnapshotRequest snapshot = request.product();
        validateProductAvailability(snapshot, request.quantity());
        Long productId = resolveProductId(snapshot);
        Long userId = user.getId();

        CartItem item = findExistingCartItem(userId, productId, snapshot.productCode())
            .orElseGet(CartItem::new);

        item.setUser(user);
        applySnapshot(item, snapshot, productId);

        int newQuantity = item.getId() == null ? request.quantity() : item.getQuantity() + request.quantity();
        validateProductAvailability(snapshot, newQuantity);
        item.setQuantity(newQuantity);
        cartRepository.save(item);
        removeDuplicateItems(userId, productId, item.getId());
        return buildResponse(cartRepository.findAllByUserId(userId));
    }

    @Transactional
    public CartResponse updateItemQuantity(String userEmail, String productCode, int quantity) {
        UserAccount user = getUser(userEmail);
        CartItem item = findByProductCode(user.getId(), productCode)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item for product '%s' was not found.".formatted(productCode)));

        validateProductAvailability(item, quantity);
        item.setQuantity(quantity);
        cartRepository.save(item);
        removeDuplicateItems(user.getId(), item.getProductId(), item.getId());
        return buildResponse(cartRepository.findAllByUserId(user.getId()));
    }

    @Transactional
    public CartResponse removeItem(String userEmail, String productCode) {
        UserAccount user = getUser(userEmail);
        List<CartItem> items = cartRepository.findAllByUserIdAndProductCode(user.getId(), productCode.trim());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Cart item for product '%s' was not found.".formatted(productCode));
        }

        cartRepository.deleteAll(items);
        return buildResponse(cartRepository.findAllByUserId(user.getId()));
    }

    @Transactional
    public void clearCart(String userEmail) {
        UserAccount user = getUser(userEmail);
        cartRepository.deleteAllByUser_Id(user.getId());
    }

    private UserAccount getUser(String userEmail) {
        return userRepository.findByEmailIgnoreCase(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User '%s' was not found.".formatted(userEmail)));
    }

    private void validateProductAvailability(ProductSnapshotRequest product, int requestedQuantity) {
        if (product.availableQuantity() <= 0) {
            throw new IllegalStateException("Product '%s' is not available for cart.".formatted(product.name()));
        }
        if (requestedQuantity > product.availableQuantity()) {
            throw new IllegalStateException(
                "Requested quantity for '%s' exceeds available stock (%s).".formatted(
                    product.name(),
                    product.availableQuantity()
                )
            );
        }
    }

    private void validateProductAvailability(CartItem item, int requestedQuantity) {
        if (item.getAvailableQuantity() <= 0) {
            throw new IllegalStateException("Product '%s' is not available for cart.".formatted(item.getProductName()));
        }
        if (requestedQuantity > item.getAvailableQuantity()) {
            throw new IllegalStateException(
                "Requested quantity for '%s' exceeds available stock (%s).".formatted(
                    item.getProductName(),
                    item.getAvailableQuantity()
                )
            );
        }
    }

    private void applySnapshot(CartItem item, ProductSnapshotRequest snapshot, Long productId) {
        item.setProductId(productId);
        item.setProductCode(snapshot.productCode().trim());
        item.setProductExternalCode(blankToNull(snapshot.externalCode()));
        item.setProductName(snapshot.name().trim());
        item.setProductCategory(blankToNull(snapshot.category()));
        item.setProductBrand(snapshot.brand().trim());
        item.setPrice(snapshot.price());
        item.setOldPrice(snapshot.oldPrice() != null ? snapshot.oldPrice() : snapshot.price());
        item.setCurrencyCode(defaultIfBlank(snapshot.currencyCode(), "KZT"));
        item.setImageUrl(blankToNull(snapshot.imageUrl()));
        item.setProductUrl(blankToNull(snapshot.productUrl()));
        item.setAvailableQuantity(snapshot.availableQuantity());
        item.setDelivery(blankToNull(snapshot.delivery()));
        item.setTag(blankToNull(snapshot.tag()));
        item.setSource(blankToNull(snapshot.source()));
    }

    private java.util.Optional<CartItem> findExistingCartItem(Long userId, Long productId, String productCode) {
        List<CartItem> byProductId = cartRepository.findAllByUserIdAndProductId(userId, productId);
        if (!byProductId.isEmpty()) {
            return java.util.Optional.of(byProductId.get(0));
        }

        if (productCode == null || productCode.isBlank()) {
            return java.util.Optional.empty();
        }

        return findByProductCode(userId, productCode);
    }

    private java.util.Optional<CartItem> findByProductCode(Long userId, String productCode) {
        List<CartItem> items = cartRepository.findAllByUserIdAndProductCode(userId, productCode.trim());
        if (items.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(items.get(0));
    }

    private void removeDuplicateItems(Long userId, Long productId, Long keepId) {
        List<CartItem> duplicates = cartRepository.findAllByUserIdAndProductId(userId, productId).stream()
            .filter(item -> !item.getId().equals(keepId))
            .toList();

        if (!duplicates.isEmpty()) {
            cartRepository.deleteAll(duplicates);
        }
    }

    private Long resolveProductId(ProductSnapshotRequest snapshot) {
        if (snapshot.productId() != null && snapshot.productId() > 0) {
            return snapshot.productId();
        }

        if (snapshot.productCode() != null && !snapshot.productCode().isBlank()) {
            return productRepository.findFirstByProductCodeIgnoreCase(snapshot.productCode().trim())
                .map(internet.magazine.magazine.product.Product::getId)
                .orElseGet(() -> resolveProductIdByExternalCode(snapshot));
        }

        return resolveProductIdByExternalCode(snapshot);
    }

    private Long resolveProductIdByExternalCode(ProductSnapshotRequest snapshot) {
        if (snapshot.externalCode() != null && !snapshot.externalCode().isBlank()) {
            return productRepository.findFirstByExternalCodeIgnoreCase(snapshot.externalCode().trim())
                .map(internet.magazine.magazine.product.Product::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Product '%s' was not found for cart.".formatted(snapshot.productCode())
                ));
        }

        throw new ResourceNotFoundException("Product '%s' was not found for cart.".formatted(snapshot.productCode()));
    }

    private CartResponse buildResponse(List<CartItem> items) {
        List<CartItemResponse> responses = items.stream()
            .map(CartItemResponse::from)
            .toList();

        int totalQuantity = responses.stream()
            .mapToInt(CartItemResponse::quantity)
            .sum();

        BigDecimal subtotal = responses.stream()
            .map(CartItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(responses, totalQuantity, subtotal);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
