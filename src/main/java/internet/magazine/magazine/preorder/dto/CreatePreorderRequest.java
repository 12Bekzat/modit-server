package internet.magazine.magazine.preorder.dto;

import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePreorderRequest(
    @NotNull @Valid ProductSnapshotRequest product,
    @NotBlank @Size(min = 2, max = 120) String contactName,
    @NotBlank @Email String contactEmail,
    @NotBlank
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Phone number contains unsupported characters.")
    String contactPhone,
    @Size(max = 1000) String comment
) {
}
