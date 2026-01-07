package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.GoogleDriveToken;
import com.bolsadeideas.springboot.app.models.entity.GoogleOAuthToken;
import com.bolsadeideas.springboot.app.models.dao.GoogleOAuthTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GoogleDriveApiService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveApiService.class);

    private final GoogleOAuthService oauthService;
    private final CloudinaryService cloudinaryService;
    private final GoogleOAuthTokenRepository googleDriveTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    public GoogleDriveApiService(GoogleOAuthService oauthService,
                                 CloudinaryService cloudinaryService,
                                 GoogleOAuthTokenRepository googleDriveTokenRepository) {
        this.oauthService = oauthService;
        this.cloudinaryService = cloudinaryService;
        this.googleDriveTokenRepository = googleDriveTokenRepository;
    }

    // ========================
    // MÉTODOS EXISTENTES (mantenidos)
    // ========================

    public byte[] downloadFileBytes(String userId, String fileId) {
        return downloadWithAutoRefresh(userId, fileId, true);
    }

    private byte[] downloadWithAutoRefresh(String userId, String fileId, boolean retryOn401) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files/{fileId}")
                .queryParam("alt", "media")
                .queryParam("supportsAllDrives", "true")
                .build(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            return resp.getBody();

        } catch (HttpClientErrorException.Unauthorized ex) {
            if (!retryOn401) throw new RuntimeException("401_TOKEN_EXPIRED");
            oauthService.getValidAccessTokenOrRefresh(userId); // fuerza refresh
            return downloadWithAutoRefresh(userId, fileId, false);

        } catch (HttpClientErrorException.Forbidden ex) {
            throw new RuntimeException("403_ACCESS_DENIED");

        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("404_NOT_FOUND");
        }
    }

    public Map<String, Object> getFileMetadata(String userId, String fileId) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files/{fileId}")
                .queryParam("fields", "id,name,mimeType,size")
                .queryParam("supportsAllDrives", "true")
                .build(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map body = resp.getBody();
        return body != null ? body : Map.of();
    }

    public List<Map<String, Object>> listFolderChildren(String userId, String folderId) {
        String accessToken = oauthService.getValidAccessTokenOrRefresh(userId);

        String q = "'" + folderId + "' in parents and trashed=false";

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://www.googleapis.com/drive/v3/files")
                .queryParam("q", q)
                .queryParam("fields", "files(id,name,mimeType,size)")
                .queryParam("supportsAllDrives", "true")
                .queryParam("includeItemsFromAllDrives", "true")
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map body = resp.getBody();
        if (body == null) return List.of();

        Object files = body.get("files");
        if (files instanceof List) {
            return (List<Map<String, Object>>) files;
        }
        return List.of();
    }

    // ========================
    // NUEVOS MÉTODOS OPTIMIZADOS CON STREAMING
    // ========================

    /**
     * Descarga un archivo de Google Drive usando streaming y lo sube directamente a Cloudinary.
     * Evita cargar el archivo completo en memoria.
     */
    private String downloadAndUploadToCloudinaryFromDrive(String fileId, String accessToken, Long npedido) throws IOException {
        log.info("📥 Iniciando descarga optimizada de Google Drive → Cloudinary - FileID: {}", fileId);

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new RuntimeException("Token de acceso vacío o nulo");
        }

        String apiUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
        URL url = new URL(apiUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(120_000);

        try {
            int responseCode = connection.getResponseCode();
            log.debug("Respuesta HTTP de Google Drive: {}", responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                switch (responseCode) {
                    case 401 -> throw new RuntimeException("GDRIVE_401_TOKEN_EXPIRED");
                    case 403 -> throw new RuntimeException("GDRIVE_403_ACCESS_DENIED");
                    case 404 -> throw new RuntimeException("GDRIVE_404_NOT_FOUND");
                    default -> throw new RuntimeException("GDRIVE_HTTP_ERROR_" + responseCode);
                }
            }

            String disposition = connection.getHeaderField("Content-Disposition");
            String fileName = "gdrive_" + npedido + "_" + System.currentTimeMillis();

            if (disposition != null && disposition.contains("filename=")) {
                String originalName = disposition.split("filename=")[1]
                        .replaceAll("\"", "")
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
                fileName = "gdrive_" + npedido + "_" + originalName;
            }

            log.info("🚀 Subiendo a Cloudinary en streaming directo: {}", fileName);

            try (InputStream driveInputStream = connection.getInputStream()) {
                String cloudinaryUrl = cloudinaryService.uploadImage(driveInputStream, npedido, fileName);

                log.info("✅ Archivo subido correctamente vía streaming: {} bytes → {}",
                        connection.getContentLengthLong(), cloudinaryUrl);

                return cloudinaryUrl;
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Flujo backend: obtiene token válido y descarga + sube con streaming.
     */
    public String downloadAndUploadToCloudinary(String userId, String fileId, Long npedido) throws IOException {
        log.info("📥 [Backend] Iniciando descarga de Drive → Cloudinary para usuario {} - FileID: {}", userId, fileId);

        String accessToken = obtenerAccessTokenValido(userId);

        return downloadAndUploadToCloudinaryFromDrive(fileId, accessToken, npedido);
    }

    /**
     * Obtiene un access_token válido, renovándolo si es necesario.
     */
    private String obtenerAccessTokenValido(String userId) {
        GoogleDriveToken tokenEntity = googleDriveTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró token de Google Drive para el usuario: " + userId));

        if (tokenEntity.getAccessToken() != null &&
                tokenEntity.getExpiresAt() != null &&
                tokenEntity.getExpiresAt().isAfter(LocalDateTime.now())) {

            log.info("🔑 Access token válido encontrado para usuario {}", userId);
            return tokenEntity.getAccessToken();
        }

        log.info("🔄 Access token expirado. Renovando con refresh_token para usuario {}", userId);

        if (tokenEntity.getRefreshToken() == null) {
            throw new RuntimeException("No hay refresh_token almacenado para el usuario: " + userId);
        }

        try {
            String tokenUrl = "https://oauth2.googleapis.com/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", tokenEntity.getRefreshToken());
            params.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String newAccessToken = (String) body.get("access_token");
                Integer expiresIn = (Integer) body.get("expires_in");

                tokenEntity.setAccessToken(newAccessToken);
                tokenEntity.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60));
                googleDriveTokenRepository.save(tokenEntity);

                log.info("✅ Access token renovado correctamente para usuario {}", userId);
                return newAccessToken;
            } else {
                throw new RuntimeException("Error renovando token: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error al renovar access_token para usuario {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("No se pudo renovar el token de Google Drive", e);
        }
    }
}