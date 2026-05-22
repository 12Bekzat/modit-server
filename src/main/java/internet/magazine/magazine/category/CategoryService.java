package internet.magazine.magazine.category;

import internet.magazine.magazine.category.dto.CategoryRequest;
import internet.magazine.magazine.category.dto.CategoryResponse;
import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.product.ProductRepository;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private static final String DEFAULT_CATEGORY_NAME = "Другое";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public List<CategoryResponse> findAll() {
        syncWithProductCategoriesIfEmpty();
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional
    public List<CategoryResponse> findVisibleNavigation() {
        syncWithProductCategoriesIfEmpty();
        return categoryRepository.findAllByVisibleTrueOrderByFeaturedDescSortOrderAscNameAsc().stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional
    public List<String> findVisibleNames() {
        return findVisibleNavigation().stream()
            .map(CategoryResponse::name)
            .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String normalizedName = normalizeRequired(request.name());
        ensureNameIsAvailable(normalizedName, null);

        Category category = new Category();
        apply(category, request, normalizedName);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String previousName = category.getName();
        String normalizedName = normalizeRequired(request.name());

        ensureNameIsAvailable(normalizedName, id);
        apply(category, request, normalizedName);
        Category savedCategory = categoryRepository.save(category);

        if (!previousName.equals(normalizedName)) {
            productRepository.replaceCategoryName(previousName, normalizedName);
        }

        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        String previousName = category.getName();
        if (DEFAULT_CATEGORY_NAME.equalsIgnoreCase(previousName)) {
            throw new IllegalStateException("Default category cannot be deleted.");
        }

        categoryRepository.delete(category);
        ensureCategoryExists(DEFAULT_CATEGORY_NAME);
        productRepository.replaceCategoryName(previousName, DEFAULT_CATEGORY_NAME);
    }

    @Transactional
    public void ensureCategoryExists(String rawName) {
        String name = normalize(rawName);
        if (name == null) {
            return;
        }

        categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setVisible(true);
            category.setFeatured(false);
            category.setSortOrder(0);
            return categoryRepository.save(category);
        });
    }

    @Transactional
    public void syncWithProductCategories() {
        Set<String> categoryNames = new LinkedHashSet<>();
        categoryNames.add(DEFAULT_CATEGORY_NAME);
        categoryNames.addAll(
            productRepository.findDistinctCategories().stream()
                .map(this::normalize)
                .filter(item -> item != null)
                .toList()
        );

        categoryNames.forEach(this::ensureCategoryExists);
    }

    private void syncWithProductCategoriesIfEmpty() {
        if (categoryRepository.count() == 0L) {
            syncWithProductCategories();
        }
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category with id '%s' was not found.".formatted(id)));
    }

    private void ensureNameIsAvailable(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name).ifPresent((existing) -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalStateException("Category with this name already exists.");
            }
        });
    }

    private void apply(Category category, CategoryRequest request, String normalizedName) {
        category.setName(normalizedName);
        category.setDescription(normalizeDescription(request.description()));
        category.setVisible(request.visible());
        category.setFeatured(request.featured());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalStateException("Category name is required.");
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
