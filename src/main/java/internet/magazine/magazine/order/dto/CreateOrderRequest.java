package internet.magazine.magazine.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank @Size(min = 2, max = 120) String firstName,
    @Size(max = 120) String lastName,
    @NotBlank
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Phone number contains unsupported characters.")
    String phone,
    @Email @Size(max = 160) String email,
    @Size(max = 2000) String comment,
    @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}
