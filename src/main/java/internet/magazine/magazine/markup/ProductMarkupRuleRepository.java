package internet.magazine.magazine.markup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMarkupRuleRepository extends JpaRepository<ProductMarkupRule, Long> {

    Optional<ProductMarkupRule> findByProductCodeIgnoreCase(String productCode);

    List<ProductMarkupRule> findAllByOrderByUpdatedAtDesc();

    List<ProductMarkupRule> findAllByProductCodeIn(Collection<String> productCodes);
}
