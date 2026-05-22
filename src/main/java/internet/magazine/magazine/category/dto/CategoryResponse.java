package internet.magazine.magazine.category.dto;

import internet.magazine.magazine.category.Category;
import java.time.Instant;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    boolean visible,
    boolean featured,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.isVisible(),
            category.isFeatured(),
            category.getSortOrder(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
