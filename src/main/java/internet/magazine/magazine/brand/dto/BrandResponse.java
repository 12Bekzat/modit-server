package internet.magazine.magazine.brand.dto;

import internet.magazine.magazine.brand.Brand;
import java.time.Instant;

public record BrandResponse(
    Long id,
    String name,
    String description,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
            brand.getId(),
            brand.getName(),
            brand.getDescription(),
            brand.getSortOrder(),
            brand.getCreatedAt(),
            brand.getUpdatedAt()
        );
    }
}
