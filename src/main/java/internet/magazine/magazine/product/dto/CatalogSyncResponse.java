package internet.magazine.magazine.product.dto;

public record CatalogSyncResponse(
    int created,
    int updated,
    int skipped
) {

    public CatalogSyncResponse add(CatalogSyncResponse other) {
        return new CatalogSyncResponse(
            created + other.created(),
            updated + other.updated(),
            skipped + other.skipped()
        );
    }
}
