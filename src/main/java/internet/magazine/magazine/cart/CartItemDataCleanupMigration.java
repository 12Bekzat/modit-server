package internet.magazine.magazine.cart;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CartItemDataCleanupMigration {

    private final JdbcTemplate jdbcTemplate;

    public CartItemDataCleanupMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void normalizeLegacyCartRows() {
        jdbcTemplate.update("update cart_items set available_quantity = 0 where available_quantity is null");
    }
}
