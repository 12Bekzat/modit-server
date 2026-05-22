package internet.magazine.magazine.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import internet.magazine.magazine.brand.BrandService;
import internet.magazine.magazine.category.CategoryService;
import internet.magazine.magazine.product.dto.CatalogImportSettingsResponse;
import internet.magazine.magazine.product.dto.CatalogSyncResponse;
import internet.magazine.magazine.product.dto.UpdateCatalogImportSettingsRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogImportService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final String DEFAULT_CATEGORY = "Другое";
    private static final String DEFAULT_BRAND = "Vend";
    private static final String DEFAULT_CURRENCY = "KZT";
    private static final String ADDITIONAL_FIELDS = "description,brand,images,url,warehouses";

    private final CatalogImportSettingsRepository settingsRepository;
    private final ProductRepository productRepository;
    private final ProductCodeGenerator productCodeGenerator;
    private final ProductSourceConstraintMigration productSourceConstraintMigration;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final Object vendRateLimitMonitor = new Object();
    private Instant lastRequestAt = Instant.EPOCH;

    public CatalogImportService(
        CatalogImportSettingsRepository settingsRepository,
        ProductRepository productRepository,
        ProductCodeGenerator productCodeGenerator,
        ProductSourceConstraintMigration productSourceConstraintMigration,
        CategoryService categoryService,
        BrandService brandService
    ) {
        this.settingsRepository = settingsRepository;
        this.productRepository = productRepository;
        this.productCodeGenerator = productCodeGenerator;
        this.productSourceConstraintMigration = productSourceConstraintMigration;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public CatalogImportSettingsResponse getSettings() {
        return CatalogImportSettingsResponse.from(getOrCreateSettings());
    }

    @Transactional
    public CatalogImportSettingsResponse updateSettings(UpdateCatalogImportSettingsRequest request) {
        CatalogImportSettings settings = getOrCreateSettings();
        settings.setApiBaseUrl(blankToNull(request.apiBaseUrl()));
        if (blankToNull(request.accessToken()) != null) {
            settings.setAccessToken(blankToNull(request.accessToken()));
        }
        if (request.pageSize() != null) {
            settings.setPageSize(Math.max(request.pageSize(), 0));
        }
        if (request.maxItems() != null) {
            settings.setMaxItems(Math.max(request.maxItems(), 0));
        }
        if (request.requestIntervalMs() != null) {
            settings.setRequestIntervalMs(Math.max(request.requestIntervalMs(), 0L));
        }
        return CatalogImportSettingsResponse.from(settingsRepository.save(settings));
    }

    @Transactional
    public CatalogSyncResponse syncAll() {
        productSourceConstraintMigration.ensureVendSourceIsAllowed();

        CatalogImportSettings settings = getOrCreateSettings();
        validateSettings(settings);

        Instant syncStartedAt = Instant.now();
        VendCategoryCache categoryCache = loadCategoryCache(settings);
        CatalogSyncResponse counters = new CatalogSyncResponse(0, 0, 0);
        int offset = 0;
        int pageSize = resolvePageSize(settings);
        int remainingItems = resolveMaxItems(settings);
        boolean hasImportLimit = remainingItems > 0;
        while (true) {
            if (hasImportLimit && remainingItems == 0) {
                break;
            }

            int requestLimit = hasImportLimit ? Math.min(pageSize, remainingItems) : pageSize;
            JsonNode payload = requestJson(
                settings,
                "/api/elements-pagination",
                Map.of(
                    "limit", String.valueOf(requestLimit),
                    "offset", String.valueOf(offset),
                    "additional_fields", ADDITIONAL_FIELDS
                )
            );
            JsonNode elements = payload.path("elements");
            if (!elements.isArray() || elements.isEmpty()) {
                break;
            }

            counters = counters.add(syncPage(elements, categoryCache, settings, syncStartedAt));
            if (hasImportLimit) {
                remainingItems -= elements.size();
            }

            JsonNode pagination = payload.path("pagination");
            int totalCount = pagination.path("totalCount").asInt(-1);
            int currentOffset = pagination.path("offset").asInt(offset);
            int responseLimit = pagination.path("limit").asInt(requestLimit);
            int nextOffset = currentOffset + Math.max(responseLimit, elements.size());
            if (totalCount >= 0 && currentOffset + elements.size() >= totalCount) {
                break;
            }
            if (elements.size() < requestLimit) {
                break;
            }
            offset = nextOffset;
        }

        List<Product> staleProducts = productRepository.findAllBySourceAndLastSyncedAtBefore(ProductSource.VEND, syncStartedAt);
        if (!staleProducts.isEmpty()) {
            productRepository.deleteAll(staleProducts);
        }

        categoryService.syncWithProductCategories();
        brandService.syncWithProductBrands();

        return new CatalogSyncResponse(
            counters.created(),
            counters.updated(),
            counters.skipped() + staleProducts.size()
        );
    }

    private CatalogSyncResponse syncPage(
        JsonNode elements,
        VendCategoryCache categoryCache,
        CatalogImportSettings settings,
        Instant syncStartedAt
    ) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<Product> batch = new ArrayList<>();

        for (JsonNode element : elements) {
            String article = text(element.path("article"));
            if (article == null) {
                skipped++;
                continue;
            }

            Product product = productRepository.findBySourceAndExternalCode(ProductSource.VEND, article)
                .orElseGet(Product::new);
            boolean isExisting = product.getId() != null;

            applyVendProduct(product, article, element, categoryCache, settings, syncStartedAt);
            batch.add(product);

            if (isExisting) {
                updated++;
            } else {
                created++;
            }
        }

        if (!batch.isEmpty()) {
            productSourceConstraintMigration.ensureVendSourceIsAllowed();
            productRepository.saveAll(batch);
        }

        return new CatalogSyncResponse(created, updated, skipped);
    }

    private void applyVendProduct(
        Product product,
        String article,
        JsonNode element,
        VendCategoryCache categoryCache,
        CatalogImportSettings settings,
        Instant syncStartedAt
    ) {
        int quantity = parseAvailability(element.path("quantity"));
        BigDecimal price = parseDecimal(element.path("price1"));
        BigDecimal oldPrice = resolveOldPrice(parseDecimal(element.path("price2")), price);
        String categoryId = text(element.path("category"));

        product.setSource(ProductSource.VEND);
        product.setExternalCode(article);
        product.setProductCode(productCodeGenerator.generateImportedCode(ProductSource.VEND, article));
        product.setName(fit(firstNonBlank(text(element.path("full_name")), text(element.path("name")), article), 160));
        product.setCategory(fit(firstNonBlank(categoryCache.idToName().get(categoryId), categoryId, DEFAULT_CATEGORY), 120));
        product.setBrand(fit(firstNonBlank(text(element.path("brand")), DEFAULT_BRAND), 120));
        product.setPrice(price);
        product.setOldPrice(oldPrice);
        product.setRating(resolveRating(element));
        product.setAvailableQuantity(quantity);
        product.setInStock(quantity > 0);
        product.setDelivery(resolveDelivery(quantity));
        product.setTag(resolveTag(quantity));
        product.setDescription(
            fit(firstNonBlank(text(element.path("description")), text(element.path("full_name")), text(element.path("name")), article), 1000)
        );
        product.setCurrencyCode(DEFAULT_CURRENCY);
        List<String> imageUrls = extractImageUrls(element.path("images"), settings);
        product.setImageUrls(imageUrls);
        product.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        product.setProductUrl(fit(text(element.path("url")), 500));
        product.setLastSyncedAt(syncStartedAt);
    }

    private VendCategoryCache loadCategoryCache(CatalogImportSettings settings) {
        JsonNode payload = requestJson(settings, "/api/categories", Map.of());
        JsonNode items = payload.isArray() ? payload : payload.path("value");
        Map<String, String> idToName = new LinkedHashMap<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = text(item.path("id"));
                String name = text(item.path("name"));
                if (id != null && name != null) {
                    idToName.put(id, name);
                }
            }
        }

        return new VendCategoryCache(idToName);
    }

    private JsonNode requestJson(CatalogImportSettings settings, String path, Map<String, String> params) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("access-token", settings.getAccessToken());
        query.putAll(params);

        HttpRequest request = HttpRequest.newBuilder(buildUri(trimTrailingSlash(settings.getApiBaseUrl()) + path, query))
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build();

        for (int attempt = 0; attempt < 3; attempt++) {
            throttleRequests(settings);

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new IllegalStateException("Catalog API returned HTTP " + response.statusCode());
                }

                JsonNode payload = objectMapper.readTree(response.body());
                if (payload.path("success").isBoolean() && !payload.path("success").asBoolean()) {
                    throw new IllegalStateException(firstNonBlank(text(payload.path("message")), "Catalog API request failed."));
                }
                return payload;
            } catch (Exception exception) {
                if (attempt >= 2) {
                    throw new IllegalStateException("Failed to import catalog from API.", exception);
                }
                sleep(resolveRequestInterval(settings));
            }
        }

        throw new IllegalStateException("Catalog API request failed after retries.");
    }

    private void throttleRequests(CatalogImportSettings settings) {
        synchronized (vendRateLimitMonitor) {
            long intervalMs = resolveRequestInterval(settings);
            long elapsedMs = Duration.between(lastRequestAt, Instant.now()).toMillis();
            long sleepMs = intervalMs - elapsedMs;
            if (sleepMs > 0) {
                sleep(sleepMs);
            }
            lastRequestAt = Instant.now();
        }
    }

    private URI buildUri(String baseUrl, Map<String, String> params) {
        StringJoiner query = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            query.add(
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
            );
        }
        return URI.create(baseUrl + "?" + query);
    }

    private CatalogImportSettings getOrCreateSettings() {
        return settingsRepository.findById(CatalogImportSettings.singletonId()).orElseGet(() -> {
            CatalogImportSettings settings = new CatalogImportSettings();
            settings.setId(CatalogImportSettings.singletonId());
            return settingsRepository.save(settings);
        });
    }

    private void validateSettings(CatalogImportSettings settings) {
        if (blankToNull(settings.getApiBaseUrl()) == null) {
            throw new IllegalStateException("Укажите базовый URL API перед импортом.");
        }
        if (blankToNull(settings.getAccessToken()) == null) {
            throw new IllegalStateException("Укажите access token перед импортом.");
        }
    }

    private int resolvePageSize(CatalogImportSettings settings) {
        if (settings.getPageSize() == null || settings.getPageSize() < 0) {
            return 250;
        }
        return settings.getPageSize();
    }

    private int resolveMaxItems(CatalogImportSettings settings) {
        if (settings.getMaxItems() == null || settings.getMaxItems() < 0) {
            return 0;
        }
        return settings.getMaxItems();
    }

    private long resolveRequestInterval(CatalogImportSettings settings) {
        if (settings.getRequestIntervalMs() == null || settings.getRequestIntervalMs() < 0) {
            return 1200L;
        }
        return settings.getRequestIntervalMs();
    }

    private BigDecimal resolveRating(JsonNode element) {
        String value = text(element.path("rating"));
        if (value == null) {
            return BigDecimal.valueOf(4.5);
        }
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException exception) {
            return BigDecimal.valueOf(4.5);
        }
    }

    private String resolveDelivery(int quantity) {
        return quantity > 0 ? "2-3 дня" : "предзаказ";
    }

    private String resolveTag(int quantity) {
        return quantity > 0 ? "В наличии" : "Предзаказ";
    }

    private BigDecimal resolveOldPrice(BigDecimal retailPrice, BigDecimal dealerPrice) {
        if (retailPrice == null || retailPrice.compareTo(dealerPrice) < 0) {
            return dealerPrice;
        }
        return retailPrice;
    }

    private BigDecimal parseDecimal(JsonNode node) {
        String value = text(node);
        if (value == null || "0".equals(value)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(" ", "").replace(",", ".").replace(">", ""));
    }

    private int parseAvailability(JsonNode node) {
        String value = text(node);
        if (value == null) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private List<String> extractImageUrls(JsonNode imagesNode, CatalogImportSettings settings) {
        if (!imagesNode.isArray() || imagesNode.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode imageNode : imagesNode) {
            String normalized = fit(normalizeImageUrl(text(imageNode), settings), 500);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return new ArrayList<>(values);
    }

    private String normalizeImageUrl(String imageUrl, CatalogImportSettings settings) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (imageUrl.startsWith("//")) {
            return "https:" + imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return trimTrailingSlash(settings.getApiBaseUrl()) + imageUrl;
        }
        return imageUrl;
    }

    private void sleep(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Catalog import was interrupted.", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalStateException("Catalog API URL is not configured.");
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String fit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record VendCategoryCache(Map<String, String> idToName) {
    }
}
