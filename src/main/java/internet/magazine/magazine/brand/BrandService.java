package internet.magazine.magazine.brand;

import internet.magazine.magazine.brand.dto.BrandRequest;
import internet.magazine.magazine.brand.dto.BrandResponse;
import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.product.ProductRepository;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BrandService {

    private static final String DEFAULT_BRAND_NAME = "Без бренда";

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandService(BrandRepository brandRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public List<BrandResponse> findAll() {
        syncWithProductBrandsIfEmpty();
        return brandRepository.findAllByOrderBySortOrderAscNameAsc().stream()
            .map(BrandResponse::from)
            .toList();
    }

    @Transactional
    public List<String> findNames() {
        return findAll().stream()
            .map(BrandResponse::name)
            .toList();
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String normalizedName = normalizeRequired(request.name());
        ensureNameIsAvailable(normalizedName, null);

        Brand brand = new Brand();
        apply(brand, request, normalizedName);
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = getBrand(id);
        String previousName = brand.getName();
        String normalizedName = normalizeRequired(request.name());

        ensureNameIsAvailable(normalizedName, id);
        apply(brand, request, normalizedName);
        Brand savedBrand = brandRepository.save(brand);

        if (!previousName.equals(normalizedName)) {
            productRepository.replaceBrandName(previousName, normalizedName);
        }

        return BrandResponse.from(savedBrand);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = getBrand(id);
        String previousName = brand.getName();
        if (DEFAULT_BRAND_NAME.equalsIgnoreCase(previousName)) {
            throw new IllegalStateException("Default brand cannot be deleted.");
        }

        brandRepository.delete(brand);
        ensureBrandExists(DEFAULT_BRAND_NAME);
        productRepository.replaceBrandName(previousName, DEFAULT_BRAND_NAME);
    }

    @Transactional
    public void ensureBrandExists(String rawName) {
        String name = normalize(rawName);
        if (name == null) {
            return;
        }

        brandRepository.findByNameIgnoreCase(name).orElseGet(() -> {
          Brand brand = new Brand();
          brand.setName(name);
          brand.setSortOrder(0);
          return brandRepository.save(brand);
        });
    }

    @Transactional
    public void syncWithProductBrands() {
        Set<String> brandNames = new LinkedHashSet<>();
        brandNames.add(DEFAULT_BRAND_NAME);
        brandNames.addAll(
            productRepository.findDistinctBrands().stream()
                .map(this::normalize)
                .filter(item -> item != null)
                .toList()
        );

        brandNames.forEach(this::ensureBrandExists);
    }

    private void syncWithProductBrandsIfEmpty() {
        if (brandRepository.count() == 0L) {
            syncWithProductBrands();
        }
    }

    private Brand getBrand(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand with id '%s' was not found.".formatted(id)));
    }

    private void ensureNameIsAvailable(String name, Long currentId) {
        brandRepository.findByNameIgnoreCase(name).ifPresent((existing) -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalStateException("Бренд с таким названием уже существует.");
            }
        });
    }

    private void apply(Brand brand, BrandRequest request, String normalizedName) {
        brand.setName(normalizedName);
        brand.setDescription(normalizeDescription(request.description()));
        brand.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalStateException("Название бренда обязательно.");
        }
        return normalized;
    }

    private String normalizeDescription(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }
}
