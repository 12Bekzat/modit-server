package internet.magazine.magazine.markup.dto;

import internet.magazine.magazine.markup.MarkupMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateCatalogMarkupSettingsRequest(
    boolean enabled,
    @NotNull MarkupMode mode,
    @NotNull @DecimalMin(value = "0.0") BigDecimal value
) {
}
