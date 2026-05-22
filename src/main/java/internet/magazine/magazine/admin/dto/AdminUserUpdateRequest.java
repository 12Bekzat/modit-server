package internet.magazine.magazine.admin.dto;

import internet.magazine.magazine.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
    @NotBlank @Size(min = 2, max = 120) String fullName,
    @NotBlank @Email String email,
    @Size(min = 8, max = 100) String password,
    @NotBlank
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Phone number contains unsupported characters.")
    String phone,
    @NotNull UserRole role
) {
}
