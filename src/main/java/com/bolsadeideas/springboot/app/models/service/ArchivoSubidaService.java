package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Log4j2
@Service
public class ArchivoSubidaService {

    private final PedidoService pedidoService;
    private final ArchivoAdjuntoService archivoAdjuntoService;
    private final CloudinaryService cloudinaryService;
    private final GoogleDriveApiService googleDriveApiService;
    private final RedisTemplate<String, String> redisTemplate;

    public ArchivoSubidaService(PedidoService pedidoService,
                                ArchivoAdjuntoService archivoAdjuntoService,
                                CloudinaryService cloudinaryService,
                                GoogleDriveApiService googleDriveApiService,
                                RedisTemplate<String, String> redisTemplate) {
        this.pedidoService = pedidoService;
        this.archivoAdjuntoService = archivoAdjuntoService;
        this.cloudinaryService = cloudinaryService;
        this.googleDriveApiService = googleDriveApiService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Procesa archivos locales y de Google Drive para un pedido.
     * Puede llamarse desde controlador HTTP o desde consumidor asíncrono.
     *
     * @param npedido Pedido al que adjuntar archivos
     * @param files Archivos locales (pueden ser null si se llama desde background)
     * @param googleDriveFileIds IDs de archivos de Drive (puede ser null o vacío)
     * @param googleDriveToken Token del frontend (puede ser null en flujo backend)
     * @param userId ID del usuario autenticado (para flujo backend, puede ser null si no aplica)
     * @return número de archivos procesados correctamente
     */
    public int procesarArchivos(Long npedido, MultipartFile[] files, String[] googleDriveFileIds,
                                String googleDriveToken, String userId) {

        log.info("🔵 ========== INICIO procesarArchivos ==========");
        log.info("📍 Pedido: {}", npedido);
        log.info("📊 Archivos locales: {}, Google Drive: {}",
                files != null ? files.length : 0,
                googleDriveFileIds != null ? googleDriveFileIds.length : 0);

        Pedido pedido = pedidoService.findOne(npedido);
        if (pedido == null) {
            log.warn("❌ Pedido {} no encontrado", npedido);
            return 0;
        }

        if ((files == null || files.length == 0) && (googleDriveFileIds == null || googleDriveFileIds.length == 0)) {
            log.info("✅ No hay archivos para procesar");
            return 0;
        }

        int archivosProcesados = 0;
        StringBuilder errores = new StringBuilder();

        // ========= ARCHIVOS LOCALES =========
        if (files != null && files.length > 0) {
            archivosProcesados += procesarArchivosLocales(npedido, files, errores);
        }

        // ========= ARCHIVOS GOOGLE DRIVE =========
        if (googleDriveFileIds != null && googleDriveFileIds.length > 0) {
            archivosProcesados += procesarArchivosGoogleDrive(npedido, googleDriveFileIds, googleDriveToken, userId, errores);
        }

        // ========= RESUMEN =========
        if (archivosProcesados > 0) {
            log.info("✅ {} elemento(s) procesado(s) para pedido {}", archivosProcesados, npedido);
        }
        if (errores.length() > 0) {
            log.warn("⚠️ Errores durante procesamiento:\n{}", errores.toString());
        }

        log.info("🎬 FIN procesarArchivos para pedido {}", npedido);
        return archivosProcesados;
    }

    // ==================== ARCHIVOS LOCALES ====================
    private int procesarArchivosLocales(Long npedido, MultipartFile[] files, StringBuilder errores) {
        int procesados = 0;

        for (MultipartFile foto : files) {
            String nombreOriginal = foto.getOriginalFilename();
            String contentType = foto.getContentType();

            log.info("🔍 Validando archivo local: {} ({} bytes, MIME: {})", nombreOriginal, foto.getSize(), contentType);

            if (foto.isEmpty()) {
                errores.append("• Archivo vacío: ").append(nombreOriginal).append("\n");
                continue;
            }

            if (!validarTipoMime(contentType, nombreOriginal)) {
                errores.append("• Tipo de archivo no soportado: ").append(nombreOriginal).append("\n");
                continue;
            }

            try (InputStream localStream = foto.getInputStream()) {
                // Nombre para Cloudinary (puedes usar el original o uno generado)
                String fileNameParaCloudinary = "pedido_" + npedido + "_" + System.currentTimeMillis() +
                        (nombreOriginal != null ? "_" + nombreOriginal : "");

                // ¡Mismo método que usas para Drive! Streaming directo
                String cloudinaryUrl = cloudinaryService.uploadImage(localStream, npedido, fileNameParaCloudinary);

                if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                    throw new RuntimeException("Cloudinary devolvió URL vacía");
                }

                // Cache y guardado en BD (común)
                String cacheKey = "local_" + npedido + "_" + fileNameParaCloudinary.replaceAll("[^a-zA-Z0-9_-]", "_");
                cachearYGuardarAdjunto(npedido, nombreOriginal, cloudinaryUrl, cacheKey);

                procesados++;
                log.info("✅ Archivo local → Cloudinary OK: {} → {}", nombreOriginal, cloudinaryUrl);

            } catch (Exception e) {
                log.error("❌ Error subiendo archivo local a Cloudinary: {}", nombreOriginal, e);
                errores.append("• Error al procesar: ").append(nombreOriginal).append("\n");
            }
        }

        return procesados;
    }

    // ==================== GOOGLE DRIVE (casi sin cambios) ====================
    private int procesarArchivosGoogleDrive(Long npedido, String[] googleDriveFileIds,
                                            String googleDriveToken, String userId, StringBuilder errores) {
        int procesados = 0;
        boolean hasFrontToken = (googleDriveToken != null && !googleDriveToken.isBlank());

        log.info("🔗 Procesando {} archivo(s) de Google Drive (token frontend? {})", googleDriveFileIds.length, hasFrontToken);

        if (!hasFrontToken && userId == null) {
            errores.append("• Usuario no autenticado para acceso backend a Drive\n");
            return 0;
        }

        for (String fileId : googleDriveFileIds) {
            if (fileId == null || fileId.trim().isEmpty()) continue;

            try {
                String cloudinaryUrl = hasFrontToken
                        ? downloadAndUploadToCloudinaryFromDrive(fileId, googleDriveToken, npedido)
                        : googleDriveApiService.downloadAndUploadToCloudinary(userId, fileId, npedido);

                if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                    errores.append("• Archivo vacío desde Drive: ").append(fileId).append("\n");
                    continue;
                }

                String cacheKey = "gdrive_" + npedido + "_" + fileId;
                String nombreDisplay = "Google Drive - " + fileId;

                cachearYGuardarAdjunto(npedido, nombreDisplay, cloudinaryUrl, cacheKey);

                procesados++;
                log.info("✅ Drive → Cloudinary OK: {} → {}", fileId, cloudinaryUrl);

            } catch (Exception e) {
                log.error("❌ Error procesando Drive fileId={}", fileId, e);
                errores.append("• Error procesando Drive (").append(fileId).append("): ").append(e.getMessage()).append("\n");
            }
        }

        return procesados;
    }

    // ==================== COMÚN: CACHE + BD ====================
    private void cachearYGuardarAdjunto(Long npedido, String nombreDisplay, String cloudinaryUrl, String cacheKey) {
        redisTemplate.opsForValue().set(cacheKey, cloudinaryUrl, Duration.ofDays(30));
        log.info("📦 URL cacheada en Redis (30 días) con clave: {}", cacheKey);

        ArchivoAdjunto adjunto = new ArchivoAdjunto(npedido, nombreDisplay, cloudinaryUrl);
        archivoAdjuntoService.guardar(adjunto);
    }


    /**
     * Descarga un archivo de Google Drive en streaming y lo sube directamente a Cloudinary.
     * Evita cargar el archivo completo en memoria (no usa byte[]).
     * Incluye medición de tiempos para depuración.
     *
     * @param fileId      ID del archivo en Google Drive
     * @param accessToken Token OAuth2 válido (Bearer)
     * @param npedido     Número de pedido para organizar en Cloudinary
     * @return URL segura del archivo subido a Cloudinary
     * @throws IOException Si hay error de red, autenticación o subida
     */
    private String downloadAndUploadToCloudinaryFromDrive(String fileId, String accessToken, Long npedido) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("📥 Iniciando descarga optimizada de Google Drive → Cloudinary - FileID: {}", fileId);

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Token de acceso vacío o nulo");
        }

        String apiUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
        URL url = new URL(apiUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(20_000);   // 20 segundos para conexión (aumentado para handshake lento)
        connection.setReadTimeout(90_000);      // 1.5 minutos lectura (para archivos grandes)

        try {
            long connectStart = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long connectEnd = System.currentTimeMillis();

            log.debug("Tiempo de conexión + respuesta HTTP: {} ms - Código: {}",
                    (connectEnd - connectStart), responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                handleDriveError(responseCode, fileId);
            }

            // Obtener nombre original del archivo desde headers (mejor que timestamp)
            String disposition = connection.getHeaderField("Content-Disposition");
            String fileName = "gdrive_" + npedido + "_" + System.currentTimeMillis();

            if (disposition != null && disposition.contains("filename=")) {
                String originalName = disposition.split("filename=")[1]
                        .replaceAll("\"", "")
                        .replaceAll("[^a-zA-Z0-9._-]", "_"); // Sanitizar
                fileName = "gdrive_" + npedido + "_" + originalName;
            }

            log.info("🚀 Subiendo directamente a Cloudinary en streaming: {}", fileName);

            // STREAMING DIRECTO: Drive → Cloudinary sin memoria intermedia
            long uploadStart = System.currentTimeMillis();
            try (InputStream driveStream = connection.getInputStream()) {
                String cloudinaryUrl = cloudinaryService.uploadImage(driveStream, npedido, fileName);

                long uploadEnd = System.currentTimeMillis();
                log.info("✅ Drive → Cloudinary OK (streaming): {} bytes → {} (tiempo: {} ms)",
                        connection.getContentLengthLong(), cloudinaryUrl, (uploadEnd - uploadStart));

                return cloudinaryUrl;
            }

        } finally {
            connection.disconnect();
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("⏱️ Tiempo total descarga + subida para FileID {}: {} segundos", fileId, totalTime / 1000.0);
        }
    }

    /**
     * Manejo de errores HTTP comunes de Google Drive.
     */
    private void handleDriveError(int responseCode, String fileId) throws RuntimeException {
        switch (responseCode) {
            case 401 -> throw new RuntimeException("GDRIVE_401_TOKEN_EXPIRED");
            case 403 -> throw new RuntimeException("GDRIVE_403_ACCESS_DENIED");
            case 404 -> throw new RuntimeException("GDRIVE_404_NOT_FOUND");
            default -> throw new RuntimeException("GDRIVE_HTTP_ERROR_" + responseCode);
        }
    }

    private boolean validarTipoMime(String contentType, String fileName) {
        if (contentType == null) {
            contentType = "";
        }

        // Tipos MIME válidos
        String[] tiposValidos = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"};
        boolean esValidoPorMime = false;

        for (String tipo : tiposValidos) {
            if (contentType.contains(tipo)) {
                esValidoPorMime = true;
                break;
            }
        }

        // Validar también por extensión del archivo (importante para Google Drive)
        String extension = fileName != null ? fileName.toLowerCase() : "";
        boolean esValidoPorExtension = extension.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$");

        // Si Google Drive no envía MIME type, validar por extensión
        if (contentType.isEmpty() || contentType.equals("application/octet-stream")) {
            log.info("📌 Archivo sin MIME type o tipo genérico. Validando por extensión: {}", extension);
            return esValidoPorExtension;
        }

        return esValidoPorMime || esValidoPorExtension;
    }
}
