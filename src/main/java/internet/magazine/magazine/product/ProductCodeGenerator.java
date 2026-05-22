package internet.magazine.magazine.product;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductCodeGenerator {

    private static final int MAX_CODE_LENGTH = 64;

    private final ProductRepository productRepository;

    public ProductCodeGenerator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String generateManualCode() {
        String candidate;
        do {
            candidate = "PRD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        } while (productRepository.existsByProductCode(candidate));
        return candidate;
    }

    public String generateImportedCode(ProductSource source, String externalCode) {
        if (externalCode == null || externalCode.isBlank()) {
            return generateManualCode();
        }

        String prefix = switch (source) {
            case IT4PROFIT -> "IT4";
            case VSTRADE -> "VST";
            case VEND -> "VND";
            case MANUAL -> "PRD";
        };
        String normalized = externalCode
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) {
            return generateManualCode();
        }

        String suffix = Integer.toUnsignedString(externalCode.trim().hashCode(), 36).toUpperCase(Locale.ROOT);
        String candidate = prefix + "-" + normalized + "-" + suffix;
        if (candidate.length() > MAX_CODE_LENGTH) {
            candidate = candidate.substring(0, MAX_CODE_LENGTH);
        }
        return candidate;
    }
}
