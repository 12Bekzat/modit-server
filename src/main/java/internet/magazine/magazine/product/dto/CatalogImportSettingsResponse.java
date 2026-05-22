package internet.magazine.magazine.product.dto;

import internet.magazine.magazine.product.CatalogImportSettings;
import java.time.Instant;

public record CatalogImportSettingsResponse(
    String apiBaseUrl,
    boolean accessTokenConfigured,
    Integer pageSize,
    Integer maxItems,
    Long requestIntervalMs,
    Instant updatedAt
) {

    public static CatalogImportSettingsResponse from(CatalogImportSettings settings) {
        return new CatalogImportSettingsResponse(
            settings.getApiBaseUrl(),
            settings.getAccessToken() != null && !settings.getAccessToken().isBlank(),
            settings.getPageSize(),
            settings.getMaxItems(),
            settings.getRequestIntervalMs(),
            settings.getUpdatedAt()
        );
    }
}
