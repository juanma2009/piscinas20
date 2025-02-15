package com.bolsadeideas.springboot.app.models.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final String cloudName;  // Agregamos la variable para almacenar el nombre del cloud


    public CloudinaryService(
            @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
            @Value("${CLOUDINARY_API_KEY}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
        this.cloudName = cloudName;  // Asignamos el nombre del cloud a la variable
    }

    public String getImageUrl(String publicId) throws Exception {
        try {
            log.info(publicId);
            // Hacer una solicitud a la API de Cloudinary para obtener los detalles del recurso
            ApiResponse result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            log.info(result.toString());
            // Verificar si la respuesta contiene la URL segura
            if (result == null || !result.containsKey("secure_url")) {
                throw new RuntimeException("No se pudo obtener la URL segura para el archivo con ID: " + publicId);
            }

            // Obtener la URL segura del resultado
            String secureUrl = (String) result.get("secure_url");
            log.info(secureUrl);
            // Retornar la URL segura
            return secureUrl;
        } catch (Exception e) {
            // Captura cualquier excepción que pueda ocurrir (API de Cloudinary, red, etc.)
            throw new RuntimeException("Error al obtener la URL de la imagen desde Cloudinary: " + e.getMessage(), e);
        }
    }


    // Método para subir una imagen
    public String uploadImage(byte[] imageBytes, String fileName) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap("public_id", fileName));
        return uploadResult.get("secure_url").toString();
    }

    // Método para eliminar una imagen
    public String deleteImage(String publicId) throws IOException {
        Map deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return deleteResult.get("result").toString();  // "ok" o "not found"
    }

    // Método para descargar una imagen
    public InputStream downloadImage(String publicId) throws IOException {
        // Utilizamos la variable cloudName directamente
        String imageUrl = new StringBuilder()
                .append("https://res.cloudinary.com/")
                .append(cloudName)  // Usamos cloudName que ya se guardó al inicio
                .append("/image/upload/")
                .append(publicId)
                .toString();
        URL url = new URL(imageUrl);
        return url.openStream();  // Regresa un InputStream de la imagen
    }
}
