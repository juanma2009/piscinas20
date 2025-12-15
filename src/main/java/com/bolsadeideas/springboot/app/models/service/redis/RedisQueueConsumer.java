package com.bolsadeideas.springboot.app.models.service.redis;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.service.ArchivoAdjuntoService;
import com.bolsadeideas.springboot.app.models.service.CloudinaryService;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
                boolean esGoogleDrive = metadatos.length > 3 && "GDRIVE".equals(metadatos[3]);

                log.info("📍 pedidoId: {}, nombreOriginal: {}, fileName: {}, esGoogleDrive: {}", pedidoId, nombreOriginal, fileName, esGoogleDrive);

                byte[] imageBytes;
                
                if (esGoogleDrive) {
                    log.info("🔗 Descargando archivo de Google Drive...");
                    String redisKeyGdrive = "gdrive_pending_" + pedidoId + "_" + fileName;
                    String googleDriveLink = (String) redisTemplate.opsForValue().get(redisKeyGdrive);
                    
                    if (googleDriveLink == null || googleDriveLink.isEmpty()) {
                        log.warn("⚠️ No se encontró link de Google Drive en Redis: {}", redisKeyGdrive);
                        return;
                    }
                    
                    imageBytes = descargarDesdeGoogleDrive(googleDriveLink);
                    
                    if (imageBytes == null || imageBytes.length == 0) {
                        log.warn("⚠️ Error al descargar desde Google Drive o archivo vacío");
                        redisTemplate.delete(redisKeyGdrive);
                        return;
                    }
                    
                    redisTemplate.delete(redisKeyGdrive);
                } else {
                    log.info("📁 Procesando archivo local...");
                    String redisKey = "file_pending_" + pedidoId + "_" + fileName;
                    String imageBase64 = (String) redisTemplate.opsForValue().get(redisKey);

                    if (imageBase64 == null || imageBase64.isEmpty()) {
                        log.warn("⚠️ No se encontró archivo en Redis: {}", redisKey);
                        return;
                    }
                    
                    imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
                    redisTemplate.delete(redisKey);
                }
                
                if (imageBytes.length == 0) {
                    log.warn("⚠️ Archivo decodificado está vacío");
                    return;
                }

                log.info("📤 Subiendo {} ({} KB) a Cloudinary en background...", nombreOriginal, imageBytes.length / 1024);
                String urlCloudinary = cloudinaryService.uploadImage(imageBytes, pedidoId, fileName);

                log.info("✅ Archivo subido a Cloudinary: {} -> {}", nombreOriginal, urlCloudinary);

                ArchivoAdjunto archivoAdjunto = new ArchivoAdjunto(pedidoId, nombreOriginal, urlCloudinary);
                archivoAdjuntoRepository.guardar(archivoAdjunto);
                log.info("✅ Metadatos guardados en BD: pedido {} - {}", pedidoId, nombreOriginal);

            } catch (Exception e) {
                log.error("❌ Error procesando mensaje en background: {}", mensaje, e);
            }
        }
    }

    private byte[] descargarDesdeGoogleDrive(String googleDriveUrl) {
        try {
            log.info("🌐 Descargando desde: {}", googleDriveUrl);
            
            String downloadUrl = convertirAUrlDescarga(googleDriveUrl);
            log.info("📥 URL de descarga convertida: {}", downloadUrl);
            
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            InputStream inputStream = connection.getInputStream();
            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();
            
            log.info("✅ Descargado {} bytes desde Google Drive", imageBytes.length);
            return imageBytes;
            
        } catch (Exception e) {
            log.error("❌ Error descargando desde Google Drive: {}", e.getMessage(), e);
            return null;
        }
    }

    private String convertirAUrlDescarga(String googleDriveUrl) {
        if (googleDriveUrl.contains("drive.google.com")) {
            String fileId = extraerFileId(googleDriveUrl);
            if (fileId != null && !fileId.isEmpty()) {
                return "https://drive.google.com/uc?export=download&id=" + fileId;
            }
        }
        return googleDriveUrl;
    }

    private String extraerFileId(String googleDriveUrl) {
        if (googleDriveUrl.contains("/d/")) {
            String[] partes = googleDriveUrl.split("/d/");
            if (partes.length > 1) {
                String fileId = partes[1].split("/")[0];
                return fileId;
            }
        }
        return null;
    }
}
