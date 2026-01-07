package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.service.ArchivoSubidaService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Log4j2
@Service
public class RedisQueueProducer {

        private final StringRedisTemplate redisTemplate;
    @Autowired
    private ArchivoSubidaService archivoSubidaService;

        public RedisQueueProducer(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        // Método para enviar un mensaje a la cola de Redis
        public void sendMessage(String mensaje) {
            redisTemplate.opsForList().leftPush("pedido-queue", mensaje);
            System.out.println("Mensaje enviado a Redis: " + mensaje);
        }


    @Scheduled(fixedDelay = 5000)
    public void procesarCola() {
        String tarea = redisTemplate.opsForList().rightPop("cola:procesar-archivos");

        if (tarea == null) return;

        String[] partes = tarea.split("\\|");
        Long npedido = Long.parseLong(partes[0]);
        String userId = partes[1];
        String[] googleDriveFileIds = partes[2].isEmpty() ? new String[0] : partes[2].split(",");
        String googleDriveToken = partes.length > 3 ? partes[3] : null;

        // Llamamos al mismo método reutilizable
        int procesados = archivoSubidaService.procesarArchivos(npedido, null, googleDriveFileIds, googleDriveToken, userId);

        log.info("✅ Procesados {} archivos en background para pedido {}", procesados, npedido);
    }
}
