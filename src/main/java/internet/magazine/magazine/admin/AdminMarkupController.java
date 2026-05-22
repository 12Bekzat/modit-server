package internet.magazine.magazine.admin;

import internet.magazine.magazine.markup.CatalogMarkupService;
import internet.magazine.magazine.markup.dto.CatalogMarkupSettingsResponse;
import internet.magazine.magazine.markup.dto.ProductMarkupRuleRequest;
import internet.magazine.magazine.markup.dto.ProductMarkupRuleResponse;
import internet.magazine.magazine.markup.dto.UpdateCatalogMarkupSettingsRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/markups")
public class AdminMarkupController {

    private final CatalogMarkupService catalogMarkupService;

    public AdminMarkupController(CatalogMarkupService catalogMarkupService) {
        this.catalogMarkupService = catalogMarkupService;
    }

    @GetMapping("/settings")
    public CatalogMarkupSettingsResponse getSettings() {
        return catalogMarkupService.getSettings();
    }

    @PutMapping("/settings")
    public CatalogMarkupSettingsResponse updateSettings(
        @Valid @RequestBody UpdateCatalogMarkupSettingsRequest request
    ) {
        return catalogMarkupService.updateSettings(request);
    }

    @GetMapping("/products")
    public List<ProductMarkupRuleResponse> findAllProductRules() {
        return catalogMarkupService.findAllProductRules();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductMarkupRuleResponse upsertProductRule(
        @Valid @RequestBody ProductMarkupRuleRequest request
    ) {
        return catalogMarkupService.upsertProductRule(request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductRule(@PathVariable Long id) {
        catalogMarkupService.deleteProductRule(id);
    }
}
