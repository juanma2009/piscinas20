package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.service.GoogleDriveApiService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/google/drive")
public class GoogleDriveController {

    private final GoogleDriveApiService driveApi;

    public GoogleDriveController(GoogleDriveApiService driveApi) {
        this.driveApi = driveApi;
    }

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<byte[]> preview(@PathVariable String fileId, Principal principal) {
        String userId = principal.getName();

        // 1) metadata (mimeType/size)
        Map<String, Object> meta = driveApi.getFileMetadata(userId, fileId);
        String mimeType = (String) meta.get("mimeType");
        Object sizeObj = meta.get("size");

        // Solo imágenes
        if (mimeType == null || !mimeType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        // (Opcional) evita previews de imágenes enormes (ej: > 15MB)
        long size = 0L;
        try {
            if (sizeObj instanceof Number) size = ((Number) sizeObj).longValue();
            else if (sizeObj instanceof String) size = Long.parseLong((String) sizeObj);
        } catch (Exception ignored) {}
        if (size > 15L * 1024L * 1024L) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        // 2) bytes (tu servicio ya auto-refresca y reintenta si 401)
        byte[] bytes = driveApi.downloadFileBytes(userId, fileId);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentDisposition(ContentDisposition.inline().filename("preview").build());
        headers.setCacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate());

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
