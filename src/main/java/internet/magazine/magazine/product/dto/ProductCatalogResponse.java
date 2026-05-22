package internet.magazine.magazine.product.dto;

import java.util.List;

public record ProductCatalogResponse(
    List<ProductResponse> items,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean hasNext
) {
}
