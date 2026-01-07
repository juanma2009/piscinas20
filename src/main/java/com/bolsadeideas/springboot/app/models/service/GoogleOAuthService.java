package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.GoogleOAuthTokenClient;
import com.bolsadeideas.springboot.app.models.dao.GoogleOAuthTokenRepository;
import com.bolsadeideas.springboot.app.models.entity.GoogleDriveToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);


    private final GoogleOAuthTokenRepository repo;
    private final GoogleOAuthTokenClient tokenClient;

    public GoogleOAuthService(GoogleOAuthTokenRepository repo, GoogleOAuthTokenClient tokenClient) {
        this.repo = repo;
        this.tokenClient = tokenClient;
    }

    /**
     * Almacena los tokens obtenidos tras autorización con código (primer login con Google).
     */
    @Transactional
    public void storeFromAuthorizationCode(String userId, String code) {
        log.info("🔐 Almacenando tokens OAuth para usuario {} tras autorización con código", userId);

        Map<String, Object> tok = tokenClient.exchangeCode(code);

        String accessToken = (String) tok.get("access_token");
        Number expiresIn = (Number) tok.get("expires_in");
        String refreshToken = (String) tok.get("refresh_token"); // Solo viene la primera vez
        String scope = (String) tok.get("scope");

        GoogleDriveToken entity = repo.findByUserId(userId)
                .orElse(new GoogleDriveToken(userId, refreshToken != null ? refreshToken : ""));

        if (refreshToken != null && !refreshToken.isBlank()) {
            entity.setRefreshToken(refreshToken);
            log.info("🔄 Refresh token almacenado para usuario {}", userId);
        }

        entity.setAccessToken(accessToken);
        long seconds = expiresIn != null ? expiresIn.longValue() : 3600L;
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(seconds));
        entity.setScope(scope);

        repo.save(entity);

        log.info("✅ Tokens OAuth almacenados correctamente para usuario {}", userId);
    }

    /**
     * Devuelve un access_token válido. Si está expirado, lo renueva automáticamente.
     */
    @Transactional
    public String getValidAccessTokenOrRefresh(String userId) {
        log.debug("🔑 Solicitando access_token válido para usuario {}", userId);

        GoogleDriveToken entity = repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("NO_GOOGLE_SESSION: el usuario no ha autorizado Google Drive"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryWithMargin = entity.getExpiresAt() != null ? entity.getExpiresAt().minusMinutes(2) : null;

        if (entity.getAccessToken() != null && expiryWithMargin != null && now.isBefore(expiryWithMargin)) {
            log.debug("✅ Access token aún válido para usuario {}", userId);
            return entity.getAccessToken();
        }

        log.info("🔄 Access token expirado o no válido. Renovando para usuario {}", userId);
        return forceRefreshAccessToken(userId);
    }

    /**
     * Fuerza la renovación del access_token usando el refresh_token almacenado.
     */
    @Transactional
    public String forceRefreshAccessToken(String userId) {
        log.info("🔄 Forzando renovación de access_token para usuario {}", userId);

        GoogleDriveToken entity = repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("NO_GOOGLE_SESSION: no hay sesión de Google"));

        if (entity.getRefreshToken() == null || entity.getRefreshToken().isBlank()) {
            throw new RuntimeException("NO_REFRESH_TOKEN: falta consentimiento offline (necesaria primera autorización con access_type=offline)");
        }

        Map<String, Object> refreshed = tokenClient.refreshAccessToken(entity.getRefreshToken());

        String newAccessToken = (String) refreshed.get("access_token");
        Number expiresIn = (Number) refreshed.get("expires_in");

        if (newAccessToken == null) {
            throw new RuntimeException("Error renovando token: no se recibió access_token");
        }

        long seconds = expiresIn != null ? expiresIn.longValue() : 3600L;

        entity.setAccessToken(newAccessToken);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(seconds));
        repo.save(entity);

        log.info("✅ Access token renovado correctamente para usuario {}", userId);
        return newAccessToken;
    }
}