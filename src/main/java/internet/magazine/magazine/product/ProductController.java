package internet.magazine.magazine.product;

import internet.magazine.magazine.product.dto.ProductCatalogResponse;
import internet.magazine.magazine.product.dto.ProductFilterOptionsResponse;
import internet.magazine.magazine.product.dto.ProductResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductCatalogResponse findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "24") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) List<String> categories,
        @RequestParam(required = false) List<String> brands,
        @RequestParam(required = false) BigDecimal priceMin,
        @RequestParam(required = false) BigDecimal priceMax,
        @RequestParam(required = false) BigDecimal ratingMin,
        @RequestParam(required = false) String delivery,
        @RequestParam(defaultValue = "true") boolean inStock,
        @RequestParam(required = false) String sort
    ) {
        return productService.findAvailableCatalog(
            search,
            categories,
            brands,
            priceMin,
            priceMax,
            ratingMin,
            delivery,
            inStock,
            sort,
            page,
            size
        );
    }

    @GetMapping("/filters")
    public ProductFilterOptionsResponse findFilterOptions() {
        return productService.findAvailableFilterOptions();
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }
}
