package internet.magazine.magazine.product.dto;

import java.util.List;

public record ProductFilterOptionsResponse(
    List<String> categories,
    List<String> brands
) {
}
