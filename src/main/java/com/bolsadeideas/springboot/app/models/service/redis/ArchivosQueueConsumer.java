package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.service.ArchivoSubidaService;
import com.bolsadeideas.springboot.app.models.service.CloudinaryService;
import com.bolsadeideas.springboot.app.models.service.PedidoService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class ArchivosQueueConsumer {

    @Autowired
    private ArchivoSubidaService archivoSubidaService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private CloudinaryService cloudinaryService;



    // Ejemplo con Redis (puedes usar RabbitMQ o Kafka)
    @EventListener(ApplicationReadyEvent.class)
    public void iniciarConsumidor() {
        // Lógica para escuchar la cola "pedido-archivos"
        // Cada mensaje: npedido;numArchivosLocales;fileIdsDrive
        // Procesar subida de archivos...
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
