package internet.magazine.magazine.cart;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<CartItem, Long> {

    @Query("select c from CartItem c where c.user.id = :userId order by c.updatedAt desc")
    List<CartItem> findAllByUserId(@Param("userId") Long userId);

    @Query("select c from CartItem c where c.user.id = :userId and c.productId = :productId order by c.updatedAt desc")
    List<CartItem> findAllByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("""
        select c from CartItem c
        where c.user.id = :userId
          and lower(c.productCode) = lower(:productCode)
        order by c.updatedAt desc
        """)
    List<CartItem> findAllByUserIdAndProductCode(@Param("userId") Long userId, @Param("productCode") String productCode);

    void deleteAllByUser_Id(Long userId);
}
