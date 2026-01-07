package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.service.CloudinaryService;
import com.bolsadeideas.springboot.app.models.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ArchivosQueueConsumer {

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
}
