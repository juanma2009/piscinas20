package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.controllers.PedidoController;
import lombok.extern.log4j.Log4j2;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.logging.Level;
import java.util.logging.Logger;

@Log4j2
@Component
@EnableScheduling
public class ArchivosRedisConsumer {


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PedidoController pedidoController;  // Inyectamos el controlador para reutilizar su método

    @Scheduled(fixedDelay = 3000)  // Cada 3 segundos
    public void procesarCola() {
        String tarea = redisTemplate.opsForList().rightPop("cola:procesar-archivos");

        if (tarea == null) return;

        try {
            String[] partes = tarea.split("\\|", 5);
            Long npedido = Long.parseLong(partes[0]);
            String userId = partes[1];
            String[] googleDriveFileIds = partes[2].isEmpty() ? new String[0] : partes[2].split(",");
            String googleDriveToken = partes.length > 3 ? partes[3] : null;
            boolean tieneArchivosLocales = partes.length > 4 && "true".equals(partes[4]);

            log.info("⚙️ Procesando tarea Redis para pedido {} - Usuario: {} - Drive: {} - Locales: {}"
            );

            // Reutilizamos tu lógica existente, pero sin petición HTTP
            pedidoController.subirArchivos(
                    npedido,
                    tieneArchivosLocales ? new MultipartFile[0] : null,  // No tenemos los archivos locales aquí
                    googleDriveFileIds,
                    googleDriveToken,
                    userId
            );

            log.info("✅ Procesamiento asíncrono completado para pedido {}");

        } catch (Exception e) {
            log.error("❌ Error procesando tarea Redis: {}", tarea, e);            // Opcional: reenviar a cola de errores
            redisTemplate.opsForList().leftPush("cola:errores-archivos", tarea);
        }
    }
}
