package com.bolsadeideas.springboot.app;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ConfigHeroku {

    private static final Logger logger = LoggerFactory.getLogger(ConfigHeroku.class);

    private final JdbcTemplate jdbcTemplate;

    public ConfigHeroku(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ejecuta un SELECT 1 cada 2 minutos (120000 ms)
     * y mide el tiempo de respuesta de la base de datos.
     */
    @Scheduled(fixedRate = 120000)
    public void keepAlive() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > 500) {
                logger.warn("⚠️ DB HealthCheck: respuesta lenta ({} ms)", elapsed);
            } else {
                logger.debug("✅ DB HealthCheck OK en {} ms", elapsed);
            }

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            logger.error("❌ DB HealthCheck fallo tras {} ms: {}", elapsed, e.getMessage());
        }
    }
}
