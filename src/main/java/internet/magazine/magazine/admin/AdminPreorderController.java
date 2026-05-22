package internet.magazine.magazine.admin;

import internet.magazine.magazine.preorder.PreorderService;
import internet.magazine.magazine.preorder.dto.PreorderResponse;
import internet.magazine.magazine.preorder.dto.UpdatePreorderStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/preorders")
public class AdminPreorderController {

    private final PreorderService preorderService;

    public AdminPreorderController(PreorderService preorderService) {
        this.preorderService = preorderService;
    }

    @GetMapping
    public List<PreorderResponse> findAll() {
        return preorderService.findAll();
    }

    @PutMapping("/{id}/status")
    public PreorderResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePreorderStatusRequest request
    ) {
        return preorderService.updateStatus(id, request.status());
    }
}
