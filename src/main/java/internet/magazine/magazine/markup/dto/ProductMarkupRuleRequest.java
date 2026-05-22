package internet.magazine.magazine.markup.dto;

import internet.magazine.magazine.markup.MarkupMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductMarkupRuleRequest(
    @NotBlank @Size(max = 64) String productCode,
    @Size(max = 160) String productName,
    boolean enabled,
    @NotNull MarkupMode mode,
    @NotNull @DecimalMin(value = "0.0") BigDecimal value
) {
}
