package internet.magazine.magazine.markup.dto;

import internet.magazine.magazine.markup.ProductMarkupRule;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductMarkupRuleResponse(
    Long id,
    String productCode,
    String productName,
    boolean enabled,
    String mode,
    BigDecimal value,
    Instant updatedAt
) {

    public static ProductMarkupRuleResponse from(ProductMarkupRule rule) {
        return new ProductMarkupRuleResponse(
            rule.getId(),
            rule.getProductCode(),
            rule.getProductName(),
            rule.isEnabled(),
            rule.getMode().name(),
            rule.getValue(),
            rule.getUpdatedAt()
        );
    }
}
