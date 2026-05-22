package internet.magazine.magazine.admin;

import internet.magazine.magazine.product.CatalogFileImportService;
import internet.magazine.magazine.product.CatalogImportService;
import internet.magazine.magazine.product.dto.CatalogImportSettingsResponse;
import internet.magazine.magazine.product.dto.CatalogSyncResponse;
import internet.magazine.magazine.product.dto.UpdateCatalogImportSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/catalog-import")
public class AdminCatalogImportController {

    private final CatalogImportService catalogImportService;
    private final CatalogFileImportService catalogFileImportService;

    public AdminCatalogImportController(
        CatalogImportService catalogImportService,
        CatalogFileImportService catalogFileImportService
    ) {
        this.catalogImportService = catalogImportService;
        this.catalogFileImportService = catalogFileImportService;
    }

    @GetMapping("/settings")
    public CatalogImportSettingsResponse getSettings() {
        return catalogImportService.getSettings();
    }

    @PutMapping("/settings")
    public CatalogImportSettingsResponse updateSettings(@Valid @RequestBody UpdateCatalogImportSettingsRequest request) {
        return catalogImportService.updateSettings(request);
    }

    @PostMapping("/sync")
    public CatalogSyncResponse sync() {
        return catalogImportService.syncAll();
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogSyncResponse importFile(@RequestParam("file") MultipartFile file) {
        return catalogFileImportService.importFile(file);
    }
}
