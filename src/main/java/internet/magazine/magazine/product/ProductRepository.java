package internet.magazine.magazine.product;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findAllByOrderByIdDesc();

    List<Product> findAllByInStockTrueOrderByIdDesc();

    Optional<Product> findBySourceAndExternalCode(ProductSource source, String externalCode);

    Optional<Product> findFirstByProductCodeIgnoreCase(String productCode);

    Optional<Product> findFirstByExternalCodeIgnoreCase(String externalCode);

    boolean existsByProductCode(String productCode);

    long countBySource(ProductSource source);

    void deleteAllBySourceIn(Collection<ProductSource> sources);

    List<Product> findAllBySourceAndLastSyncedAtBefore(ProductSource source, Instant lastSyncedAt);

    @Query("select distinct p.category from Product p where p.inStock = true and p.category is not null order by p.category asc")
    List<String> findDistinctAvailableCategories();

    @Query("select distinct p.category from Product p where p.category is not null and p.category <> '' order by p.category asc")
    List<String> findDistinctCategories();

    @Query("select distinct p.brand from Product p where p.brand is not null and p.brand <> '' order by p.brand asc")
    List<String> findDistinctBrands();

    @Query("select distinct p.brand from Product p where p.inStock = true and p.brand is not null order by p.brand asc")
    List<String> findDistinctAvailableBrands();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.category = :nextCategory where p.category = :currentCategory")
    int replaceCategoryName(@Param("currentCategory") String currentCategory, @Param("nextCategory") String nextCategory);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.brand = :nextBrand where p.brand = :currentBrand")
    int replaceBrandName(@Param("currentBrand") String currentBrand, @Param("nextBrand") String nextBrand);
}
