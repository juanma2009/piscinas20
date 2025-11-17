package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.service.ArchivoAdjuntoService;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueConsumer {

    private final  ArchivoAdjuntoService archivoAdjuntoRepository;
    private final StringRedisTemplate redisTemplate;

    public RedisQueueConsumer(ArchivoAdjuntoService archivoAdjuntoRepository, StringRedisTemplate redisTemplate) {
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
        this.redisTemplate = redisTemplate;
    }

    // Este método se ejecutará periódicamente para procesar los mensajes de Redis
    @Scheduled(fixedDelay = 5000)  // Procesa cada 5 segundos
    public void procesarMensaje() {
        String mensaje = redisTemplate.opsForList().rightPop("pedido-queue");

        if (mensaje != null) {
            System.out.println("Procesando mensaje: " + mensaje);
            // Extraemos los metadatos del mensaje
            String[] metadatos = mensaje.split(";");
            Long pedidoId = Long.parseLong(metadatos[0]);
            String nombreArchivo = metadatos[1];
            String urlCloudinary = metadatos[2];

            // Guardar los metadatos en la base de datos

            ArchivoAdjunto archivoAdjunto = new ArchivoAdjunto(pedidoId, nombreArchivo, urlCloudinary);
            archivoAdjuntoRepository.guardar(archivoAdjunto);
            // Aquí puedes agregar más lógica, como enviar notificaciones o realizar un procesamiento adicional

        }
    }
}
