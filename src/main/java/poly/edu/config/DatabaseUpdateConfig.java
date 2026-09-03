package poly.edu.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class DatabaseUpdateConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUpdateConfig.class);

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner updateDatabaseSchema() {
        return args -> {
            try {
                String dbProductName = "";
                if (jdbcTemplate.getDataSource() != null) {
                    try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                        dbProductName = conn.getMetaData().getDatabaseProductName();
                    }
                }
                boolean isPostgres = dbProductName.toLowerCase().contains("postgres");

                if (isPostgres) {
                    // PostgreSQL schema updates
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(255)");
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255)");
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(255)");

                    jdbcTemplate.execute("ALTER TABLE user_vouchers ADD COLUMN IF NOT EXISTS status VARCHAR(255) DEFAULT 'AVAILABLE'");
                    jdbcTemplate.execute("ALTER TABLE user_vouchers ADD COLUMN IF NOT EXISTS reservation_expires_at TIMESTAMP");
                    jdbcTemplate.execute("ALTER TABLE user_vouchers ADD COLUMN IF NOT EXISTS saved_at TIMESTAMP");
                    jdbcTemplate.execute("ALTER TABLE user_vouchers ADD COLUMN IF NOT EXISTS used_at TIMESTAMP");

                    jdbcTemplate.execute("ALTER TABLE flash_sales ADD COLUMN IF NOT EXISTS banner_image VARCHAR(500)");

                    jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS product_images (" +
                            "id SERIAL PRIMARY KEY, " +
                            "product_id INTEGER NOT NULL, " +
                            "image_url TEXT NOT NULL, " +
                            "display_order INTEGER DEFAULT 0, " +
                            "CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE)");
                    log.info("[DB] PostgreSQL schema verified/updated successfully.");
                } else {
                    // SQL Server schema updates
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'auth_provider' AND Object_ID = Object_ID(N'users')) BEGIN ALTER TABLE users ADD auth_provider VARCHAR(255) END");
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'google_id' AND Object_ID = Object_ID(N'users')) BEGIN ALTER TABLE users ADD google_id VARCHAR(255) END");
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'facebook_id' AND Object_ID = Object_ID(N'users')) BEGIN ALTER TABLE users ADD facebook_id VARCHAR(255) END");

                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'status' AND Object_ID = Object_ID(N'user_vouchers')) BEGIN ALTER TABLE user_vouchers ADD status VARCHAR(255) DEFAULT 'AVAILABLE' NOT NULL END");
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'reservation_expires_at' AND Object_ID = Object_ID(N'user_vouchers')) BEGIN ALTER TABLE user_vouchers ADD reservation_expires_at DATETIME2 END");
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'saved_at' AND Object_ID = Object_ID(N'user_vouchers')) BEGIN ALTER TABLE user_vouchers ADD saved_at DATETIME2 END");
                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'used_at' AND Object_ID = Object_ID(N'user_vouchers')) BEGIN ALTER TABLE user_vouchers ADD used_at DATETIME2 END");

                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'banner_image' AND Object_ID = Object_ID(N'flash_sales')) BEGIN ALTER TABLE flash_sales ADD banner_image VARCHAR(500) END");

                    jdbcTemplate.execute(
                            "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'product_images') AND type in (N'U')) " +
                            "BEGIN CREATE TABLE product_images (id INT IDENTITY(1,1) PRIMARY KEY, product_id INT NOT NULL FOREIGN KEY REFERENCES products(id) ON DELETE CASCADE, image_url NVARCHAR(MAX) NOT NULL, display_order INT DEFAULT 0) END");
                    log.info("[DB] SQL Server schema verified/updated successfully.");
                }

                // Remove translation table
                try {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS translations");
                    log.info("[DB] Dropped translations table successfully.");
                } catch (Exception e) {
                    log.warn("[DB] Could not drop translations table: {}", e.getMessage());
                }
            } catch (Exception e) {
                log.warn("[DB] Schema update skipped (columns/tables may already exist): {}", e.getMessage());
            }
        };
    }
}
