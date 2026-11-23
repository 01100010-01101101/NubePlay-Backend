package cl.nubeplay.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Ajuste para bases MySQL creadas antes de agregar el login con Google:
 * Hibernate (ddl-auto=update) agrega columnas nuevas, pero no quita un NOT NULL existente.
 * Las cuentas creadas con Google no traen RUN, así que la columna usuario.run debe aceptar NULL.
 */
@Component
@Order(0)
public class AjusteEsquema implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteEsquema.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public AjusteEsquema(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try (Connection conexion = dataSource.getConnection()) {
            if (!conexion.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql")) return;
            String nullable = jdbc.queryForObject(
                    "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'usuario' AND COLUMN_NAME = 'run'", String.class);
            if ("NO".equalsIgnoreCase(nullable)) {
                jdbc.execute("ALTER TABLE usuario MODIFY run VARCHAR(12) NULL");
                log.info("Columna usuario.run ahora acepta NULL (cuentas de Google).");
            }
        } catch (Exception ex) {
            log.warn("No se pudo revisar la columna usuario.run: {}", ex.getMessage());
        }
    }
}
