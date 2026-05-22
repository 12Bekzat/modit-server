package internet.magazine.magazine.cart;

import internet.magazine.magazine.cart.dto.AddCartItemRequest;
import internet.magazine.magazine.cart.dto.CartResponse;
import internet.magazine.magazine.cart.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse findCurrentCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.findCurrentCart(jwt.getSubject());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(jwt.getSubject(), request);
    }

    @PutMapping("/items/{productCode}")
    public CartResponse updateItem(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String productCode,
        @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItemQuantity(jwt.getSubject(), productCode, request.quantity());
    }

    @DeleteMapping("/items/{productCode}")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable String productCode) {
        return cartService.removeItem(jwt.getSubject(), productCode);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(jwt.getSubject());
    }
}
