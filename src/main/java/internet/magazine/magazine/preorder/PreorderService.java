package internet.magazine.magazine.preorder;

import internet.magazine.magazine.common.ResourceNotFoundException;
import internet.magazine.magazine.preorder.dto.CreatePreorderRequest;
import internet.magazine.magazine.preorder.dto.PreorderResponse;
import internet.magazine.magazine.product.dto.ProductSnapshotRequest;
import internet.magazine.magazine.user.UserAccount;
import internet.magazine.magazine.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreorderService {

    private final PreorderRepository preorderRepository;
    private final UserRepository userRepository;

    public PreorderService(
        PreorderRepository preorderRepository,
        UserRepository userRepository
    ) {
        this.preorderRepository = preorderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PreorderResponse create(CreatePreorderRequest request, String userEmail) {
        ProductSnapshotRequest product = request.product();

        if (product.availableQuantity() > 0) {
            throw new IllegalStateException("Preorder is available only for products with zero stock.");
        }

        PreorderRequestEntity entity = new PreorderRequestEntity();
        entity.setProductCode(product.productCode().trim());
        entity.setProductName(product.name().trim());
        entity.setProductExternalCode(blankToNull(product.externalCode()));
        entity.setProductBrand(blankToNull(product.brand()));
        entity.setImageUrl(blankToNull(product.imageUrl()));
        entity.setAvailableQuantity(product.availableQuantity());
        entity.setContactName(request.contactName().trim());
        entity.setContactEmail(request.contactEmail().trim().toLowerCase());
        entity.setContactPhone(request.contactPhone().trim());
        entity.setComment(blankToNull(request.comment()));
        entity.setStatus(PreorderStatus.NEW);

        if (userEmail != null) {
            UserAccount user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
            entity.setUser(user);
        }

        return PreorderResponse.from(preorderRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<PreorderResponse> findAll() {
        return preorderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(PreorderResponse::from)
            .toList();
    }

    @Transactional
    public PreorderResponse updateStatus(Long id, PreorderStatus status) {
        PreorderRequestEntity entity = preorderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Preorder with id '%s' was not found.".formatted(id)));
        entity.setStatus(status);
        return PreorderResponse.from(preorderRepository.save(entity));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
