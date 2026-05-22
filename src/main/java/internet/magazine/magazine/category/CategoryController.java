package internet.magazine.magazine.category;

import internet.magazine.magazine.category.dto.CategoryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/navigation")
    public List<CategoryResponse> findVisibleNavigation() {
        return categoryService.findVisibleNavigation();
    }
}
