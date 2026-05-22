package internet.magazine.magazine.markup;

import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.markup.dto.CatalogMarkupSettingsResponse;
import internet.magazine.magazine.markup.dto.ProductMarkupRuleRequest;
import internet.magazine.magazine.markup.dto.ProductMarkupRuleResponse;
import internet.magazine.magazine.markup.dto.UpdateCatalogMarkupSettingsRequest;
import internet.magazine.magazine.product.dto.ProductResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogMarkupService {

    private final CatalogMarkupSettingsRepository settingsRepository;
    private final ProductMarkupRuleRepository productMarkupRuleRepository;

    public CatalogMarkupService(
        CatalogMarkupSettingsRepository settingsRepository,
        ProductMarkupRuleRepository productMarkupRuleRepository
    ) {
        this.settingsRepository = settingsRepository;
        this.productMarkupRuleRepository = productMarkupRuleRepository;
    }

    @Transactional(readOnly = true)
    public CatalogMarkupSettingsResponse getSettings() {
        return CatalogMarkupSettingsResponse.from(getOrCreateSettings());
    }

    @Transactional
    public CatalogMarkupSettingsResponse updateSettings(UpdateCatalogMarkupSettingsRequest request) {
        CatalogMarkupSettings settings = getOrCreateSettings();
        settings.setEnabled(request.enabled());
        settings.setMode(request.mode());
        settings.setValue(normalize(request.value()));
        return CatalogMarkupSettingsResponse.from(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public List<ProductMarkupRuleResponse> findAllProductRules() {
        return productMarkupRuleRepository.findAllByOrderByUpdatedAtDesc()
            .stream()
            .map(ProductMarkupRuleResponse::from)
            .toList();
    }

    @Transactional
    public ProductMarkupRuleResponse upsertProductRule(ProductMarkupRuleRequest request) {
        ProductMarkupRule rule = productMarkupRuleRepository.findByProductCodeIgnoreCase(request.productCode().trim())
            .orElseGet(ProductMarkupRule::new);
        rule.setProductCode(request.productCode().trim().toUpperCase());
        rule.setProductName(blankToNull(request.productName()));
        rule.setEnabled(request.enabled());
        rule.setMode(request.mode());
        rule.setValue(normalize(request.value()));
        return ProductMarkupRuleResponse.from(productMarkupRuleRepository.save(rule));
    }

    @Transactional
    public void deleteProductRule(Long id) {
        ProductMarkupRule rule = productMarkupRuleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Markup rule with id '%s' was not found.".formatted(id)));
        productMarkupRuleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> applyMarkups(List<ProductResponse> products) {
        if (products.isEmpty()) {
            return products;
        }

        CatalogMarkupSettings settings = getOrCreateSettings();
        Map<String, ProductMarkupRule> rulesByCode = new HashMap<>();
        productMarkupRuleRepository.findAllByProductCodeIn(
                products.stream()
                    .map(ProductResponse::productCode)
                    .filter(code -> code != null && !code.isBlank())
                    .toList()
            )
            .forEach(rule -> rulesByCode.put(rule.getProductCode(), rule));

        return products.stream()
            .map(product -> applyMarkup(product, settings, rulesByCode.get(product.productCode())))
            .toList();
    }

    private ProductResponse applyMarkup(ProductResponse product, CatalogMarkupSettings settings, ProductMarkupRule rule) {
        if (product.price() == null) {
            return product;
        }

        MarkupMode mode = null;
        BigDecimal value = BigDecimal.ZERO;

        if (rule != null && rule.isEnabled()) {
            mode = rule.getMode();
            value = rule.getValue();
        } else if (settings.isEnabled()) {
            mode = settings.getMode();
            value = settings.getValue();
        }

        if (mode == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return product;
        }

        BigDecimal markedPrice = applyAmount(product.price(), mode, value);
        BigDecimal markedOldPrice = product.oldPrice() != null
            ? applyAmount(product.oldPrice(), mode, value)
            : markedPrice;

        return new ProductResponse(
            product.id(),
            product.productCode(),
            product.name(),
            product.category(),
            product.brand(),
            markedPrice,
            markedOldPrice.compareTo(markedPrice) < 0 ? markedPrice : markedOldPrice,
            product.rating(),
            product.inStock(),
            product.availableQuantity(),
            product.delivery(),
            product.tag(),
            product.description(),
            product.source(),
            product.externalCode(),
            product.currencyCode(),
            product.imageUrl(),
            product.imageUrls(),
            product.productUrl(),
            product.createdAt(),
            product.updatedAt(),
            product.lastSyncedAt()
        );
    }

    private BigDecimal applyAmount(BigDecimal basePrice, MarkupMode mode, BigDecimal value) {
        BigDecimal result = switch (mode) {
            case PERCENT -> basePrice.add(basePrice.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            case FIXED -> basePrice.add(value);
        };
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private CatalogMarkupSettings getOrCreateSettings() {
        return settingsRepository.findById(1L).orElseGet(CatalogMarkupSettings::new);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
