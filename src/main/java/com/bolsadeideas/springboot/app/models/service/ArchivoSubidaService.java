package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.ArchivoAdjunto;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.redis.RedisQueueProducer;
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

@Log4j2
@Service
public class ArchivoSubidaService {

    private final PedidoService pedidoService;
    private final ArchivoAdjuntoService archivoAdjuntoService;
    private final CloudinaryService cloudinaryService;
    private final GoogleDriveApiService googleDriveApiService;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisQueueProducer redisQueueProducer;

    public ArchivoSubidaService(PedidoService pedidoService,
                                ArchivoAdjuntoService archivoAdjuntoService,
                                CloudinaryService cloudinaryService,
                                GoogleDriveApiService googleDriveApiService,
                                RedisTemplate<String, String> redisTemplate,
                                RedisQueueProducer redisQueueProducer) {
        this.pedidoService = pedidoService;
        this.archivoAdjuntoService = archivoAdjuntoService;
        this.cloudinaryService = cloudinaryService;
        this.googleDriveApiService = googleDriveApiService;
        this.redisTemplate = redisTemplate;
        this.redisQueueProducer = redisQueueProducer;
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

        log.info("🔵 ========== INICIO procesarArchivos (reutilizable) ==========");
        log.info("📍 Pedido: {}", npedido);
        log.info("📊 Archivos locales: {}, FileIds Google Drive: {}",
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

        log.info("⚡ Procesando archivos...");
        log.info("🔐 googleDriveToken recibido? {}", (googleDriveToken != null && !googleDriveToken.isBlank()));

        // ========= ARCHIVOS LOCALES =========
        if (files != null && files.length > 0) {
            for (MultipartFile foto : files) {
                String nombreOriginal = foto.getOriginalFilename();
                String contentType = foto.getContentType();

                log.info("🔍 Validando archivo local: {} ({} bytes, MIME: {})", nombreOriginal, foto.getSize(), contentType);

                if (foto.isEmpty()) {
                    log.warn("❌ Archivo vacío: {}", nombreOriginal);
                    errores.append("• Archivo vacío: ").append(nombreOriginal).append("\n");
                    continue;
                }

                if (!validarTipoMime(contentType, nombreOriginal)) {
                    log.warn("❌ Tipo MIME inválido: {}", contentType);
                    errores.append("• Tipo de archivo no soportado: ").append(nombreOriginal).append("\n");
                    continue;
                }

                try {
                    byte[] imageBytes = foto.getBytes();
                    if (imageBytes.length == 0) {
                        errores.append("• Archivo sin contenido: ").append(nombreOriginal).append("\n");
                        continue;
                    }

                    String fileName = "pedido_" + npedido + "_" + System.currentTimeMillis();
                    String redisKey = "file_pending_" + npedido + "_" + fileName;
                    String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
                    redisTemplate.opsForValue().set(redisKey, imageBase64);
                    redisTemplate.expire(redisKey, Duration.ofHours(24));

                    String mensaje = npedido + ";" + nombreOriginal + ";" + fileName;
                    redisQueueProducer.sendMessage(mensaje);

                    archivosProcesados++;
                    log.info("✅ Archivo local encolado: {}", nombreOriginal);

                } catch (Exception e) {
                    log.error("❌ Error procesando archivo local: {}", nombreOriginal, e);
                    errores.append("• Error al procesar: ").append(nombreOriginal).append("\n");
                }
            }
        }

        // ========= ARCHIVOS DE GOOGLE DRIVE =========
        if (googleDriveFileIds != null && googleDriveFileIds.length > 0) {
            boolean hasFrontToken = (googleDriveToken != null && !googleDriveToken.isBlank());
            log.info("🔗 Procesando {} archivo(s) de Google Drive (token frontend? {})", googleDriveFileIds.length, hasFrontToken);

            if (!hasFrontToken && userId == null) {
                errores.append("• Usuario no autenticado para acceso backend a Drive\n");
            } else {
                for (String fileId : googleDriveFileIds) {
                    if (fileId == null || fileId.trim().isEmpty()) continue;

                    try {
                        log.info("📥 Descargando y subiendo directamente a Cloudinary desde Drive: {}", fileId);

                        String cloudinaryUrl;

                        if (hasFrontToken) {
                            // Flujo antiguo: token del frontend
                            cloudinaryUrl = downloadAndUploadToCloudinaryFromDrive(fileId, googleDriveToken, npedido);
                        } else {
                            // Flujo nuevo: backend gestiona el token
                            cloudinaryUrl = googleDriveApiService.downloadAndUploadToCloudinary(userId, fileId, npedido);
                        }

                        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                            log.warn("⚠️ Subida vacía/devuelta null para fileId: {}", fileId);
                            errores.append("• Archivo vacío desde Drive: ").append(fileId).append("\n");
                            continue;
                        }

                        // ← AÑADE AQUÍ EL CACHE EN REDIS
                        String fileNameCache = "gdrive_" + npedido + "_" + fileId;  // Clave única y predecible (mejor que timestamp)
                        redisTemplate.opsForValue().set(fileNameCache, cloudinaryUrl);
                        redisTemplate.expire(fileNameCache, Duration.ofDays(30));  // 30 días de cache

                        log.info("📦 URL de archivo Drive cacheada en Redis con clave: {}", fileNameCache);

                        ArchivoAdjunto adjunto = new ArchivoAdjunto(npedido, "Google Drive - " + fileId, cloudinaryUrl);
                        archivoAdjuntoService.guardar(adjunto);

                        archivosProcesados++;
                        log.info("✅ Drive → Cloudinary OK (streaming directo): {} → {}", fileId, cloudinaryUrl);

                    } catch (RuntimeException e) {
                        log.error("❌ Error Drive fileId={} msg={}", fileId, e.getMessage());
                        errores.append("• Error Drive (").append(fileId).append("): ").append(e.getMessage()).append("\n");
                    } catch (Exception e) {
                        log.error("❌ Error procesando Drive fileId={}", fileId, e);
                        errores.append("• Error procesando Drive (").append(fileId).append("): ").append(e.getMessage()).append("\n");
                    }
                }
            }
        }

        // ========= RESUMEN FINAL (logs) =========
        if (archivosProcesados > 0) {
            log.info("✅ {} elemento(s) procesado(s) para pedido {}", archivosProcesados, npedido);
        }

        if (errores.length() > 0) {
            log.warn("⚠️ Errores encontrados durante procesamiento:\n{}", errores.toString());
        }

        log.info("🎬 FIN procesarArchivos para pedido {}", npedido);
        return archivosProcesados;
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
}
