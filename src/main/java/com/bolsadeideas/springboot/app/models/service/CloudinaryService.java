package com.bolsadeideas.springboot.app.models.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String uploadPreset;
    private final StringRedisTemplate redisTemplate;

    public CloudinaryService(
            @Value("${spring.cloudinary.cloud-name}") String cloudName,
            @Value("${spring.cloudinary.api-key}") String apiKey,
            @Value("${spring.cloudinary.api-secret}") String apiSecret,
            @Value("${cloudinary.upload-preset}") String uploadPreset,
            StringRedisTemplate redisTemplate) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.uploadPreset = uploadPreset;
        this.redisTemplate = redisTemplate;
    }


    /**
     * Sube una imagen a Cloudinary usando byte[] (flujo antiguo).
     */
    public String uploadImage(byte[] imageBytes, Long npedido, String fileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Los bytes de la imagen no pueden ser nulos o vacíos");
        }

        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            return uploadImageFromStream(inputStream, npedido, fileName);
        }
    }

    /**
     * Sube una imagen a Cloudinary usando InputStream (streaming directo - MÁS RÁPIDO y eficiente).
     * Usado para descargas directas desde Google Drive sin cargar en memoria.
     */
    public String uploadImage(InputStream inputStream, Long npedido, String fileName) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("El InputStream no puede ser nulo");
        }

        return uploadImageFromStream(inputStream, npedido, fileName);
    }

    /**
     * Método común para ambos flujos: sube usando streaming.
     * Usa uploadLarge para archivos > 10MB.
     */
    private String uploadImageFromStream(InputStream inputStream, Long npedido, String fileName) throws IOException {
        try {
            log.info("📤 Iniciando upload a Cloudinary (streaming): fileName={}, pedido={}", fileName, npedido);

            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("public_id", fileName);
            uploadParams.put("folder", "pedidos/" + npedido);
            uploadParams.put("overwrite", true);
            uploadParams.put("resource_type", "auto");  // Detecta imagen, pdf, etc.

            log.debug("   Parámetros Cloudinary: {}", uploadParams);

            // uploadLarge es obligatorio para archivos > 10MB y recomendado para streaming
            Map uploadResult = cloudinary.uploader().uploadLarge(inputStream, uploadParams);

            log.debug("   Respuesta Cloudinary: {}", uploadResult);

            Object errorObj = uploadResult.get("error");
            if (errorObj != null) {
                String errorMsg = errorObj.toString();
                log.error("❌ Error de Cloudinary: {}", errorMsg);
                throw new IOException("Cloudinary error: " + errorMsg);
            }

            String imageUrl = (String) uploadResult.get("secure_url");

            if (imageUrl == null || imageUrl.isEmpty()) {
                log.error("❌ Cloudinary no retornó secure_url");
                log.error("   Respuesta completa: {}", uploadResult);
                throw new IOException("Cloudinary no retornó URL segura");
            }

            // Cache en Redis (opcional, pero útil)
            redisTemplate.opsForValue().set(fileName, imageUrl);

            log.info("✅ Imagen subida correctamente a Cloudinary: {} → {}", fileName, imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("❌ Error de IO al subir a Cloudinary: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado al subir a Cloudinary: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new IOException("Error al subir a Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Método para obtener la URL de la imagen desde Cloudinary.
     * Si la imagen no está en Redis, la obtiene de Cloudinary y la almacena en Redis.
     *
     * @param publicId El ID público de la imagen.
     * @return La URL de la imagen.
     */
    public String obtenerImagen(String publicId) throws IOException {
        if (publicId == null || publicId.trim().isEmpty()) {
            log.warn("El publicId proporcionado es nulo o vacío.");
            return "/img/default.jpg";  // Devuelve una imagen por defecto si no hay publicId.
        }

        // Primero intentamos obtener la URL desde Redis
        String cachedUrl = redisTemplate.opsForValue().get(publicId);

        if (cachedUrl != null) {
            log.info("URL encontrada en Redis para el publicId: {}", publicId);
            return cachedUrl;  // Devuelve la URL desde el caché
        }

        // Si no está en Redis, la obtenemos de Cloudinary
        log.info("URL no encontrada en Redis, obteniendo de Cloudinary.");
        return getImageUrlFromCloudinary(publicId);  // Este método obtiene la URL de Cloudinary y la guarda en el caché.
    }

    /**
     * Método para obtener la URL de la imagen desde Cloudinary.
     * @param publicId El ID público de la imagen.
     * @return La URL segura de la imagen desde Cloudinary.
     */
    public String getImageUrlFromCloudinary(String publicId) throws IOException {
        try {
            if (publicId == null || publicId.isEmpty()) {
                log.error("publicId es nulo o vacío");
                return "/img/default.jpg";
            }

            redisTemplate.opsForValue().set(publicId, publicId);
            log.info("URL retornada directamente: " + publicId);

            return publicId;
        } catch (Exception e) {
            log.error("Error al procesar la URL: " + e.getMessage(), e);
            return "/img/default.jpg";
        }
    }

    /**
     * Método para eliminar una imagen de Cloudinary.
     * @param publicId El ID público de la imagen.
     * @return El resultado de la eliminación ("ok" o "not found").
     */
    public String deleteImage(String publicId) throws IOException {
        Map deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return deleteResult.get("result").toString();  // "ok" o "not found"
    }

    /**
     * Método para descargar una imagen de Cloudinary.
     * @param publicId El ID público de la imagen.
     * @return Un InputStream con la imagen.
     */
    public InputStream downloadImage(String publicId) throws IOException {
        // Construir la URL de Cloudinary
        String imageUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId + ".jpg";
        URL url = new URL(imageUrl);
        return url.openStream();  // Regresa un InputStream de la imagen
    }

    /**
     * Generar el publicId a partir del número de pedido y el nombre del archivo.
     * @param npedido El número del pedido.
     * @param fileName El nombre del archivo.
     * @return El publicId generado.
     */
    public String generatePublicId(Long npedido, String fileName) {
        // Generar un publicId único basado en npedido y fileName
        String baseFileName = fileName.replaceAll("[^a-zA-Z0-9]", "_");  // Reemplaza caracteres no alfanuméricos por "_"
        // Generar el publicId sin duplicados
        return "pedido_" + npedido + "_" + System.currentTimeMillis() + "_" + baseFileName;
    }


    public String getImageUrl(String publicId) throws IOException {
        if (publicId == null || publicId.trim().isEmpty()) {
            log.warn("El publicId proporcionado es nulo o vacío.");
            return "/img/default.jpg";  // Devuelve una imagen por defecto si no hay publicId.
        }

        // Primero intentamos obtener la URL desde Redis
        String cachedUrl = redisTemplate.opsForValue().get(publicId);

        if (cachedUrl != null) {
            log.info("URL encontrada en Redis para el publicId: {}", publicId);
            return cachedUrl;  // Devuelve la URL desde el caché
        }

        // Si no está en Redis, la obtenemos de Cloudinary
        log.info("URL no encontrada en Redis, obteniendo de Cloudinary.");
        return getImageUrlFromCloudinary(publicId);  // Este método obtiene la URL de Cloudinary y la guarda en el caché.
    }

    public Map<String, Object> generateUploadSignature(Long npedido) {
        try {
            String folder = "pedidos/" + npedido;
            
            Map<String, Object> result = new HashMap<>();
            result.put("cloud_name", cloudName);
            result.put("upload_preset", uploadPreset);
            result.put("folder", folder);
            
            log.info("Credenciales de Cloudinary generadas para pedido {}", npedido);
            return result;
            
        } catch (Exception e) {
            log.error("Error al generar credenciales de Cloudinary: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private String generateSHA1(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec key = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA1");
        mac.init(key);
        byte[] bytes = mac.doFinal(input.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

}
