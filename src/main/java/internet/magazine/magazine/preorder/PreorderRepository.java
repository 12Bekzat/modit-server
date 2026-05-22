package internet.magazine.magazine.preorder;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreorderRepository extends JpaRepository<PreorderRequestEntity, Long> {

    List<PreorderRequestEntity> findAllByOrderByCreatedAtDesc();
}
