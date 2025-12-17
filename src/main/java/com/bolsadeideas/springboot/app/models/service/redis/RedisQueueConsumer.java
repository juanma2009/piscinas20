package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.service.ArchivoAdjuntoService;
import com.bolsadeideas.springboot.app.models.service.CloudinaryService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Log4j2
public class RedisQueueConsumer {

    private final ArchivoAdjuntoService archivoAdjuntoRepository;
    private final StringRedisTemplate redisTemplate;
    private final CloudinaryService cloudinaryService;

    public RedisQueueConsumer(ArchivoAdjuntoService archivoAdjuntoRepository, StringRedisTemplate redisTemplate, CloudinaryService cloudinaryService) {
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
        this.redisTemplate = redisTemplate;
        this.cloudinaryService = cloudinaryService;
    }

    @Scheduled(fixedDelay = 2000)
    public void procesarMensaje() {
        String mensaje = redisTemplate.opsForList().rightPop("pedido-queue");

        if (mensaje != null) {
            log.info("⚡ BACKGROUND CONSUMER: Procesando mensaje: {}", mensaje);
            try {
                String[] metadatos = mensaje.split(";");
                Long pedidoId = Long.parseLong(metadatos[0]);
                String nombreOriginal = metadatos[1];
                String fileName = metadatos[2];

                log.info("📍 pedidoId: {}, archivo: {}", pedidoId, nombreOriginal);

                log.info("📁 Procesando archivo local...");
                String redisKey = "file_pending_" + pedidoId + "_" + fileName;
                String imageBase64 = (String) redisTemplate.opsForValue().get(redisKey);

                if (imageBase64 == null || imageBase64.isEmpty()) {
                    log.warn("⚠️ No se encontró archivo en Redis: {}", redisKey);
                    return;
                }
                
                byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
                redisTemplate.delete(redisKey);
                
                if (imageBytes.length == 0) {
                    log.warn("⚠️ Archivo decodificado está vacío");
                    return;
                }

                log.info("📤 Subiendo {} ({} KB) a Cloudinary...", nombreOriginal, imageBytes.length / 1024);
                
                String urlCloudinary = null;
                try {
                    urlCloudinary = cloudinaryService.uploadImage(imageBytes, pedidoId, fileName);
                } catch (IOException e) {
                    log.error("❌ Error de IO al subir a Cloudinary: {}", e.getMessage(), e);
                    return;
                } catch (Exception e) {
                    log.error("❌ Error inesperado: {}", e.getMessage(), e);
                    return;
                }

                if (urlCloudinary == null || urlCloudinary.isEmpty()) {
                    log.error("❌ Cloudinary retornó URL vacía");
                    return;
                }

                log.info("✅ Archivo subido a Cloudinary: {} -> {}", nombreOriginal, urlCloudinary);

                ArchivoAdjunto archivoAdjunto = new ArchivoAdjunto(pedidoId, nombreOriginal, urlCloudinary);
                archivoAdjuntoRepository.guardar(archivoAdjunto);
                log.info("✅ Guardado en BD: pedido {} - {}", pedidoId, nombreOriginal);

            } catch (IllegalArgumentException e) {
                log.error("❌ Error de argumentos: {}", e.getMessage());
            } catch (Exception e) {
                log.error("❌ Error procesando mensaje: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
