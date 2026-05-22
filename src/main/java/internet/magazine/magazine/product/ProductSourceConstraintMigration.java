package internet.magazine.magazine.product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductSourceConstraintMigration {

    private static final String CONSTRAINT_NAME = "products_source_check";
    private static final String EXPECTED_CONSTRAINT_FRAGMENT = "'VEND'";
    private static final String PRODUCTS_TABLE = "products";
    private static final String CONSTRAINT_SQL =
        "check (source in ('MANUAL', 'IT4PROFIT', 'VSTRADE', 'VEND'))";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public ProductSourceConstraintMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureVendSourceIsAllowed() {
        if (!isPostgreSql()) {
            return;
        }

        String definition = jdbcTemplate.query(
            """
            select pg_get_constraintdef(c.oid)
            from pg_constraint c
            join pg_class t on t.oid = c.conrelid
            where c.conname = ?
              and t.relname = ?
            fetch first 1 row only
            """,
            rs -> rs.next() ? rs.getString(1) : null,
            CONSTRAINT_NAME,
            PRODUCTS_TABLE
        );

        if (definition == null || definition.contains(EXPECTED_CONSTRAINT_FRAGMENT)) {
            return;
        }

        jdbcTemplate.execute("alter table " + PRODUCTS_TABLE + " drop constraint if exists " + CONSTRAINT_NAME);
        jdbcTemplate.execute("alter table " + PRODUCTS_TABLE + " add constraint " + CONSTRAINT_NAME + " " + CONSTRAINT_SQL);
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null &&
                databaseProductName.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect database metadata.", exception);
        }
    }
}
