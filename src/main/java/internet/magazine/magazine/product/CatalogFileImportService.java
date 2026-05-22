package internet.magazine.magazine.product;

import internet.magazine.magazine.brand.BrandService;
import internet.magazine.magazine.category.CategoryService;
import internet.magazine.magazine.product.dto.CatalogSyncResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Service
public class CatalogFileImportService {

    private static final BigDecimal DEFAULT_RATING = BigDecimal.valueOf(4.5);
    private static final String DEFAULT_CURRENCY = "KZT";
    private static final String DEFAULT_DELIVERY = "2-3 дня";
    private static final String PREORDER_DELIVERY = "предзаказ";
    private static final String IN_STOCK_TAG = "В наличии";
    private static final String PREORDER_TAG = "Предзаказ";
    private static final String DEFAULT_CATEGORY = "Другое";
    private static final String DEFAULT_BRAND = "Без бренда";

    private static final String FIELD_CODE = "code";
    private static final String FIELD_ARTICLE = "article";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_QUALITY = "quality";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_QUANTITY = "quantity";

    private static final Map<String, Set<String>> FIELD_ALIASES = Map.of(
        FIELD_CODE, Set.of("код", "code", "id", "uid", "externalcode"),
        FIELD_ARTICLE, Set.of("артикул", "article", "sku", "vendorcode", "productcode"),
        FIELD_CATEGORY, Set.of("категория", "category", "group", "раздел"),
        FIELD_BRAND, Set.of("производитель", "brand", "vendor", "manufacturer"),
        FIELD_NAME, Set.of("номенклатура", "наименование", "name", "title", "productname", "товар"),
        FIELD_QUALITY, Set.of("качество", "quality", "condition", "состояние"),
        FIELD_PRICE, Set.of("дилерскаяkzt", "дилерскаяцена", "цена", "price", "dealerprice", "dealerkzt"),
        FIELD_QUANTITY, Set.of("остаток", "quantity", "stock", "qty", "availablequantity", "наличие")
    );

    private final ProductRepository productRepository;
    private final ProductCodeGenerator productCodeGenerator;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public CatalogFileImportService(
        ProductRepository productRepository,
        ProductCodeGenerator productCodeGenerator,
        CategoryService categoryService,
        BrandService brandService
    ) {
        this.productRepository = productRepository;
        this.productCodeGenerator = productCodeGenerator;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @Transactional
    public CatalogSyncResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("Выберите файл для импорта.");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        return switch (extension) {
            case "xls", "xlsx" -> importExcel(file);
            case "xml" -> importXml(file);
            default -> throw new IllegalStateException("Поддерживаются только .xls, .xlsx и .xml файлы.");
        };
    }

    private CatalogSyncResponse importExcel(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            HeaderMapping headerMapping = resolveHeaderMapping(sheet, formatter);
            List<ImportedCatalogRow> rows = new ArrayList<>();

            for (int rowIndex = headerMapping.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                ImportedCatalogRow importedRow = parseExcelRow(row, headerMapping.indexes(), formatter);
                if (importedRow.isEmpty()) {
                    continue;
                }
                rows.add(importedRow);
            }

            return upsertRows(rows);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось обработать Excel-файл.", exception);
        }
    }

    private CatalogSyncResponse importXml(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Document document = factory.newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();

            List<ImportedCatalogRow> rows = new ArrayList<>();
            collectXmlRows(document.getDocumentElement(), rows);
            if (rows.isEmpty()) {
                throw new IllegalStateException("В XML не найдены строки товаров.");
            }

            return upsertRows(rows);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось обработать XML-файл.", exception);
        }
    }

    private CatalogSyncResponse upsertRows(List<ImportedCatalogRow> rows) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        Instant importedAt = Instant.now();

        for (ImportedCatalogRow row : rows) {
            try {
                Optional<Product> existingProduct = findExistingProduct(row);
                Product product = existingProduct.orElseGet(Product::new);
                boolean isExisting = product.getId() != null;

                applyImportedRow(product, row, importedAt);
                productRepository.save(product);

                if (isExisting) {
                    updated++;
                } else {
                    created++;
                }
            } catch (Exception exception) {
                skipped++;
            }
        }

        categoryService.syncWithProductCategories();
        brandService.syncWithProductBrands();
        return new CatalogSyncResponse(created, updated, skipped);
    }

    private Optional<Product> findExistingProduct(ImportedCatalogRow row) {
        if (row.article() != null) {
            Optional<Product> byProductCode = productRepository.findFirstByProductCodeIgnoreCase(row.article());
            if (byProductCode.isPresent()) {
                return byProductCode;
            }
        }

        if (row.code() != null) {
            Optional<Product> byExternalCode = productRepository.findFirstByExternalCodeIgnoreCase(row.code());
            if (byExternalCode.isPresent()) {
                return byExternalCode;
            }
        }

        if (row.article() != null) {
            return productRepository.findBySourceAndExternalCode(ProductSource.VSTRADE, row.article());
        }

        return Optional.empty();
    }

    private void applyImportedRow(Product product, ImportedCatalogRow row, Instant importedAt) {
        int quantity = parseQuantity(row.quantity());
        BigDecimal price = parsePrice(row.price());
        boolean isNewProduct = product.getId() == null;

        if (isNewProduct) {
            product.setSource(ProductSource.VSTRADE);
        }
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(resolveImportedProductCode(row));
        }
        if (row.code() != null) {
            product.setExternalCode(row.code());
        }

        product.setName(fit(firstNonBlank(row.name(), row.article(), row.code(), "Без названия"), 160));
        product.setCategory(fit(firstNonBlank(row.category(), product.getCategory(), DEFAULT_CATEGORY), 120));
        product.setBrand(fit(firstNonBlank(row.brand(), product.getBrand(), DEFAULT_BRAND), 120));
        product.setPrice(price);
        product.setOldPrice(price);
        product.setRating(DEFAULT_RATING);
        product.setAvailableQuantity(quantity);
        product.setInStock(quantity > 0);
        product.setDelivery(quantity > 0 ? DEFAULT_DELIVERY : PREORDER_DELIVERY);
        product.setTag(resolveTag(row.quality(), quantity));
        product.setDescription(fit(buildDescription(row), 1000));
        product.setCurrencyCode(DEFAULT_CURRENCY);
        product.setLastSyncedAt(importedAt);
    }

    private HeaderMapping resolveHeaderMapping(Sheet sheet, DataFormatter formatter) {
        int bestRowIndex = -1;
        Map<String, Integer> bestMapping = Map.of();

        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 10); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Map<String, Integer> currentMapping = new LinkedHashMap<>();
            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                String key = normalizeKey(formatter.formatCellValue(row.getCell(cellIndex)));
                for (Map.Entry<String, Set<String>> entry : FIELD_ALIASES.entrySet()) {
                    if (entry.getValue().contains(key)) {
                        currentMapping.putIfAbsent(entry.getKey(), cellIndex);
                    }
                }
            }

            if (currentMapping.size() > bestMapping.size()) {
                bestMapping = currentMapping;
                bestRowIndex = rowIndex;
            }
        }

        if (bestRowIndex < 0 || bestMapping.size() < 4) {
            throw new IllegalStateException("Не удалось определить заголовки Excel-файла.");
        }

        return new HeaderMapping(bestRowIndex, bestMapping);
    }

    private ImportedCatalogRow parseExcelRow(Row row, Map<String, Integer> indexes, DataFormatter formatter) {
        return new ImportedCatalogRow(
            sanitizeCode(readCell(row, indexes.get(FIELD_CODE), formatter)),
            sanitizeArticle(readCell(row, indexes.get(FIELD_ARTICLE), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_CATEGORY), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_BRAND), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_NAME), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_QUALITY), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_PRICE), formatter)),
            trimToNull(readCell(row, indexes.get(FIELD_QUANTITY), formatter))
        );
    }

    private void collectXmlRows(Element element, List<ImportedCatalogRow> rows) {
        ImportedCatalogRow row = parseXmlElement(element);
        if (!row.isEmpty()) {
            rows.add(row);
            return;
        }

        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                collectXmlRows(childElement, rows);
            }
        }
    }

    private ImportedCatalogRow parseXmlElement(Element element) {
        Map<String, String> directChildren = new LinkedHashMap<>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element childElement)) {
                continue;
            }
            directChildren.put(normalizeKey(childElement.getTagName()), trimToNull(childElement.getTextContent()));
        }

        if (directChildren.isEmpty()) {
            return ImportedCatalogRow.empty();
        }

        String code = findAliasValue(directChildren, FIELD_CODE);
        String article = findAliasValue(directChildren, FIELD_ARTICLE);
        String category = findAliasValue(directChildren, FIELD_CATEGORY);
        String brand = findAliasValue(directChildren, FIELD_BRAND);
        String name = findAliasValue(directChildren, FIELD_NAME);
        String quality = findAliasValue(directChildren, FIELD_QUALITY);
        String price = findAliasValue(directChildren, FIELD_PRICE);
        String quantity = findAliasValue(directChildren, FIELD_QUANTITY);

        ImportedCatalogRow row = new ImportedCatalogRow(
            sanitizeCode(code),
            sanitizeArticle(article),
            category,
            brand,
            name,
            quality,
            price,
            quantity
        );

        return countFilledFields(row) >= 3 ? row : ImportedCatalogRow.empty();
    }

    private int countFilledFields(ImportedCatalogRow row) {
        int count = 0;
        if (row.code() != null) count++;
        if (row.article() != null) count++;
        if (row.category() != null) count++;
        if (row.brand() != null) count++;
        if (row.name() != null) count++;
        if (row.price() != null) count++;
        if (row.quantity() != null) count++;
        return count;
    }

    private String findAliasValue(Map<String, String> values, String field) {
        for (String alias : FIELD_ALIASES.get(field)) {
            String value = values.get(alias);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveImportedProductCode(ImportedCatalogRow row) {
        String article = row.article();
        if (article != null && !article.isBlank() && !productRepository.existsByProductCode(article)) {
            return article;
        }

        return productCodeGenerator.generateImportedCode(ProductSource.VSTRADE, firstNonBlank(row.code(), row.article()));
    }

    private String buildDescription(ImportedCatalogRow row) {
        String quality = trimToNull(row.quality());
        String name = firstNonBlank(row.name(), row.article(), row.code(), "Товар");
        if (quality == null) {
            return name;
        }
        return name + ". Состояние: " + quality;
    }

    private String resolveTag(String quality, int quantity) {
        String normalizedQuality = trimToNull(quality);
        if (normalizedQuality != null) {
            return fit(normalizedQuality, 40);
        }
        return quantity > 0 ? IN_STOCK_TAG : PREORDER_TAG;
    }

    private String readCell(Row row, Integer cellIndex, DataFormatter formatter) {
        if (cellIndex == null || cellIndex < 0) {
            return null;
        }
        return trimToNull(formatter.formatCellValue(row.getCell(cellIndex)));
    }

    private BigDecimal parsePrice(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return BigDecimal.ZERO;
        }
        String digits = normalized
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .replaceAll("[^0-9.\\-]", "");
        if (digits.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(digits);
    }

    private int parseQuantity(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return 0;
        }
        String digits = normalized.replaceAll("[^0-9\\-]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private String sanitizeCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replace("\u00A0", "").replace(" ", "");
    }

    private String sanitizeArticle(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return fit(normalized.toUpperCase(Locale.ROOT), 64);
    }

    private String normalizeKey(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "";
        }
        return normalized
            .toLowerCase(Locale.ROOT)
            .replace("\u00A0", "")
            .replaceAll("[\\s_\\-./()]+", "");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private String resolveExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private record HeaderMapping(int rowIndex, Map<String, Integer> indexes) {
    }

    private record ImportedCatalogRow(
        String code,
        String article,
        String category,
        String brand,
        String name,
        String quality,
        String price,
        String quantity
    ) {
        private static ImportedCatalogRow empty() {
            return new ImportedCatalogRow(null, null, null, null, null, null, null, null);
        }

        private boolean isEmpty() {
            return code == null && article == null && category == null && brand == null && name == null && price == null && quantity == null;
        }
    }
}
