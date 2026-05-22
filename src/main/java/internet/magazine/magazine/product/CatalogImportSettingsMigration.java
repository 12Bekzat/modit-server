package internet.magazine.magazine.product;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CatalogImportSettingsMigration {

    private final JdbcTemplate jdbcTemplate;

    public CatalogImportSettingsMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchema() {
        jdbcTemplate.execute(
            """
            alter table if exists catalog_import_settings
            add column if not exists max_items integer not null default 0
            """
        );

        jdbcTemplate.update("update catalog_import_settings set page_size = 250 where page_size is null or page_size < 0");
        jdbcTemplate.update("update catalog_import_settings set max_items = 0 where max_items is null or max_items < 0");
        jdbcTemplate.update(
            "update catalog_import_settings set request_interval_ms = 1200 where request_interval_ms is null or request_interval_ms < 0"
        );
        jdbcTemplate.update("update catalog_import_settings set updated_at = coalesce(updated_at, now())");
        jdbcTemplate.update("update catalog_import_settings set created_at = coalesce(created_at, updated_at, now())");
    }
}
