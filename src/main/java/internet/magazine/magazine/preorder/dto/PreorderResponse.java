package internet.magazine.magazine.preorder.dto;

import internet.magazine.magazine.preorder.PreorderRequestEntity;
import java.time.Instant;

public record PreorderResponse(
    Long id,
    String productCode,
    String productName,
    String productExternalCode,
    String productBrand,
    String imageUrl,
    int availableQuantity,
    String contactName,
    String contactEmail,
    String contactPhone,
    String comment,
    String status,
    Instant createdAt,
    Instant updatedAt,
    String userEmail
) {

    public static PreorderResponse from(PreorderRequestEntity entity) {
        return new PreorderResponse(
            entity.getId(),
            entity.getProductCode(),
            entity.getProductName(),
            entity.getProductExternalCode(),
            entity.getProductBrand(),
            entity.getImageUrl(),
            entity.getAvailableQuantity(),
            entity.getContactName(),
            entity.getContactEmail(),
            entity.getContactPhone(),
            entity.getComment(),
            entity.getStatus().name(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getUser() != null ? entity.getUser().getEmail() : null
        );
    }
}
