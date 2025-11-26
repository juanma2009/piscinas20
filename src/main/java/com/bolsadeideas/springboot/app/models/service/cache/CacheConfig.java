package com.bolsadeideas.springboot.app.models.service.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching  // Habilitar caché en Spring Boot
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Usamos Redis como la fuente de caché
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(5000)     // Limitar el número de entradas en el caché
                        .expireAfterWrite(10, TimeUnit.MINUTES)  // Opcional: expirar después de 10 minutos
        );
        return cacheManager;
    }
}
