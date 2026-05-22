package internet.magazine.magazine.preorder;

import internet.magazine.magazine.preorder.dto.CreatePreorderRequest;
import internet.magazine.magazine.preorder.dto.PreorderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preorders")
public class PreorderController {

    private final PreorderService preorderService;

    public PreorderController(PreorderService preorderService) {
        this.preorderService = preorderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreorderResponse create(
        @Valid @RequestBody CreatePreorderRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return preorderService.create(request, jwt != null ? jwt.getSubject() : null);
    }
}
