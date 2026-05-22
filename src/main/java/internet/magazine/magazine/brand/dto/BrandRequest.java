package internet.magazine.magazine.brand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
    @NotBlank @Size(min = 2, max = 120) String name,
    @Size(max = 180) String description,
    Integer sortOrder
) {
}
