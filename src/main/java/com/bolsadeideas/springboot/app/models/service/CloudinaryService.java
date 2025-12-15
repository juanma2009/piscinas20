package com.bolsadeideas.springboot.app.models.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
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
    private final StringRedisTemplate redisTemplate;

    public CloudinaryService(
            @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
            @Value("${CLOUDINARY_API_KEY}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET}") String apiSecret,
            StringRedisTemplate redisTemplate) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Método para subir una imagen a Cloudinary y almacenar su URL en el caché de Redis.
     *
     * @param imageBytes Los bytes de la imagen.
     * @param npedido El número del pedido.
     * @param fileName El nombre del archivo para Cloudinary.
     * @return La URL de la imagen subida a Cloudinary.
     */
    @Cacheable(value = "cloudinaryImages", key = "#fileName")
    public String uploadImage(byte[] imageBytes, Long npedido, String fileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Los bytes de la imagen no pueden ser nulos o vacíos");
        }

        // Subir la imagen a Cloudinary con el publicId generado
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("public_id", fileName);  // Usamos un publicId personalizado
        Map uploadResult = cloudinary.uploader().upload(imageBytes, uploadParams);
        String imageUrl = (String) uploadResult.get("secure_url");

        // Almacenar la URL en Redis con el publicId
        redisTemplate.opsForValue().set(fileName, imageUrl);

        log.info("Imagen subida a Cloudinary con publicId: {} y URL almacenada en Redis: {}", fileName, imageUrl);
        return imageUrl;
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
            // Hacer una solicitud a la API de Cloudinary para obtener los detalles del recurso
            ApiResponse result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            log.info("Resultado de Cloudinary: " + result.toString());

            // Verificar si la respuesta contiene la URL segura
            if (result == null || !result.containsKey("secure_url")) {
                log.error("No se pudo obtener la URL segura para el archivo con ID: " + publicId);
                return "/img/default.jpg";  // Si no se encuentra, devolvemos una URL por defecto
            }

            // Obtener la URL segura del resultado
            String secureUrl = (String) result.get("secure_url");
            log.info("URL segura obtenida de Cloudinary: " + secureUrl);

            // Guardar la URL en el caché de Redis para futuras consultas
            redisTemplate.opsForValue().set(publicId, secureUrl);
            log.info("URL guardada en caché para el publicId: " + publicId);

            return secureUrl;
        } catch (Exception e) {
            // Captura cualquier excepción que pueda ocurrir (API de Cloudinary, red, etc.)
            log.error("Error al obtener la URL de la imagen desde Cloudinary: " + e.getMessage(), e);
            return "/img/default.jpg";  // Si hay un error, devolvemos una URL por defecto
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
            long timestamp = System.currentTimeMillis() / 1000;
            String folder = "pedidos/" + npedido;
            
            TreeMap<String, Object> params = new TreeMap<>();
            params.put("timestamp", String.valueOf(timestamp));
            params.put("folder", folder);
            params.put("resource_type", "auto");
            
            StringBuilder toSign = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (toSign.length() > 0) toSign.append("&");
                toSign.append(entry.getKey()).append("=").append(entry.getValue());
            }
            
            String signature = generateSHA1(toSign.toString() + apiSecret);
            
            Map<String, Object> result = new HashMap<>();
            result.put("signature", signature);
            result.put("timestamp", timestamp);
            result.put("api_key", apiKey);
            result.put("cloud_name", cloudName);
            result.put("folder", folder);
            
            log.info("Firma de Cloudinary generada para pedido {}", npedido);
            return result;
            
        } catch (Exception e) {
            log.error("Error al generar firma de Cloudinary: {}", e.getMessage(), e);
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
