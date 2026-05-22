package internet.magazine.magazine.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank @Size(min = 2, max = 120) String name,
    @Size(max = 180) String description,
    boolean visible,
    boolean featured,
    Integer sortOrder
) {
}
