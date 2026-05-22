package internet.magazine.magazine.markup.dto;

import internet.magazine.magazine.markup.CatalogMarkupSettings;
import java.math.BigDecimal;
import java.time.Instant;

public record CatalogMarkupSettingsResponse(
    boolean enabled,
    String mode,
    BigDecimal value,
    Instant updatedAt
) {

    public static CatalogMarkupSettingsResponse from(CatalogMarkupSettings settings) {
        return new CatalogMarkupSettingsResponse(
            settings.isEnabled(),
            settings.getMode().name(),
            settings.getValue(),
            settings.getUpdatedAt()
        );
    }
}
