package internet.magazine.magazine.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCatalogImportSettingsRequest(
    @Size(max = 500) String apiBaseUrl,
    @Size(max = 500) String accessToken,
    @Min(0) Integer pageSize,
    @Min(0) Integer maxItems,
    @Min(0) Long requestIntervalMs
) {
}
