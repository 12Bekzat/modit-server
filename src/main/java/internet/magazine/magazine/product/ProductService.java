package internet.magazine.magazine.product;

import internet.magazine.magazine.brand.BrandService;
import internet.magazine.magazine.category.CategoryService;
import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.markup.CatalogMarkupService;
import internet.magazine.magazine.product.dto.ProductCatalogResponse;
import internet.magazine.magazine.product.dto.ProductFilterOptionsResponse;
import internet.magazine.magazine.product.dto.ProductRequest;
import internet.magazine.magazine.product.dto.ProductResponse;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCodeGenerator productCodeGenerator;
    private final CatalogMarkupService catalogMarkupService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public ProductService(
        ProductRepository productRepository,
        ProductCodeGenerator productCodeGenerator,
        CatalogMarkupService catalogMarkupService,
        CategoryService categoryService,
        BrandService brandService
    ) {
        this.productRepository = productRepository;
        this.productCodeGenerator = productCodeGenerator;
        this.catalogMarkupService = catalogMarkupService;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAvailable() {
        Page<Product> page = productRepository.findAll(
            availableCatalogSpecification(null, List.of(), List.of(), null, null, null, null, true),
            PageRequest.of(0, 24, resolveSort("popular"))
        );
        return catalogMarkupService.applyMarkups(page.getContent().stream().map(ProductResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public ProductCatalogResponse findAvailableCatalog(
        String search,
        List<String> categories,
        List<String> brands,
        BigDecimal priceMin,
        BigDecimal priceMax,
        BigDecimal ratingMin,
        String delivery,
        boolean inStock,
        String sort,
        int page,
        int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Page<Product> result = productRepository.findAll(
            availableCatalogSpecification(search, categories, brands, priceMin, priceMax, ratingMin, delivery, inStock),
            PageRequest.of(normalizedPage, normalizedSize, resolveSort(sort))
        );
        List<ProductResponse> items = catalogMarkupService.applyMarkups(
            result.getContent().stream().map(ProductResponse::from).toList()
        );

        return new ProductCatalogResponse(
            items,
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize(),
            result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public ProductFilterOptionsResponse findAvailableFilterOptions() {
        return new ProductFilterOptionsResponse(
            categoryService.findVisibleNames(),
            brandService.findNames()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdDesc()
            .stream()
            .map(ProductResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        product.setSource(ProductSource.MANUAL);
        product.setProductCode(productCodeGenerator.generateManualCode());
        Product savedProduct = productRepository.save(product);
        categoryService.ensureCategoryExists(savedProduct.getCategory());
        brandService.ensureBrandExists(savedProduct.getBrand());
        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        apply(product, request);
        if (product.getProductCode() == null || product.getProductCode().isBlank()) {
            product.setProductCode(productCodeGenerator.generateManualCode());
        }
        Product savedProduct = productRepository.save(product);
        categoryService.ensureCategoryExists(savedProduct.getCategory());
        brandService.ensureBrandExists(savedProduct.getBrand());
        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product with id '%s' was not found.".formatted(id)));
    }

    private Specification<Product> availableCatalogSpecification(
        String search,
        List<String> categories,
        List<String> brands,
        BigDecimal priceMin,
        BigDecimal priceMax,
        BigDecimal ratingMin,
        String delivery,
        boolean inStock
    ) {
        List<String> normalizedCategories = normalizeList(categories);
        List<String> normalizedBrands = normalizeList(brands);
        String normalizedSearch = normalize(search);
        String normalizedDelivery = normalize(delivery);

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (inStock) {
                predicates.add(criteriaBuilder.isTrue(root.get("inStock")));
                predicates.add(criteriaBuilder.greaterThan(root.get("availableQuantity"), 0));
            }
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(
                    criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("externalCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tag")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("productCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("delivery")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("currencyCode")), pattern)
                    )
                );
            }
            if (!normalizedCategories.isEmpty()) {
                predicates.add(root.get("category").in(normalizedCategories));
            }
            if (!normalizedBrands.isEmpty()) {
                predicates.add(root.get("brand").in(normalizedBrands));
            }
            if (priceMin != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            if (ratingMin != null && ratingMin.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), ratingMin));
            }
            if (normalizedDelivery != null && !"all".equalsIgnoreCase(normalizedDelivery)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("delivery")), normalizedDelivery.toLowerCase(Locale.ROOT)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort resolveSort(String sort) {
        String normalizedSort = normalize(sort);
        if ("price-asc".equalsIgnoreCase(normalizedSort)) {
            return Sort.by(Sort.Order.asc("price"), Sort.Order.desc("rating"), Sort.Order.desc("id"));
        }
        if ("price-desc".equalsIgnoreCase(normalizedSort)) {
            return Sort.by(Sort.Order.desc("price"), Sort.Order.desc("rating"), Sort.Order.desc("id"));
        }
        if ("newest".equalsIgnoreCase(normalizedSort)) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }
        return Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setCategory(request.category().trim());
        product.setBrand(request.brand().trim());
        product.setPrice(request.price());
        product.setOldPrice(request.oldPrice());
        product.setRating(request.rating());
        product.setAvailableQuantity(request.availableQuantity());
        product.setInStock(request.inStock() && request.availableQuantity() > 0);
        product.setDelivery(request.delivery().trim());
        product.setTag(request.tag().trim());
        product.setDescription(request.description().trim());
        List<String> imageUrls = normalizeImageUrls(request.imageUrls(), request.imageUrl());
        product.setImageUrls(imageUrls);
        product.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        product.setProductUrl(blankToNull(request.productUrl()));
        product.setCurrencyCode(blankToNull(request.currencyCode()));
    }

    private List<String> normalizeImageUrls(List<String> imageUrls, String imageUrl) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        String primaryImage = blankToNull(imageUrl);

        if (primaryImage != null) {
            normalized.add(primaryImage);
        }

        if (imageUrls != null) {
            imageUrls.stream()
                .map(this::blankToNull)
                .filter(item -> item != null)
                .forEach(normalized::add);
        }

        return new ArrayList<>(normalized);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .map(this::normalize)
            .filter(item -> item != null)
            .distinct()
            .toList();
    }
}
