package internet.magazine.magazine.brand;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findAllByOrderBySortOrderAscNameAsc();

    Optional<Brand> findByNameIgnoreCase(String name);
}
