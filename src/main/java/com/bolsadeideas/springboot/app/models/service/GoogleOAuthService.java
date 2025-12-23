package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.GoogleOAuthTokenClient;
import com.bolsadeideas.springboot.app.models.dao.GoogleOAuthTokenRepository;
import com.bolsadeideas.springboot.app.models.entity.GoogleOAuthToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class GoogleOAuthService {

    private final GoogleOAuthTokenRepository repo;
    private final GoogleOAuthTokenClient tokenClient;

    public GoogleOAuthService(GoogleOAuthTokenRepository repo, GoogleOAuthTokenClient tokenClient) {
        this.repo = repo;
        this.tokenClient = tokenClient;
    }

    @Transactional
    public void storeFromAuthorizationCode(String userId, String code) {
        Map<String, Object> tok = tokenClient.exchangeCode(code);

        String accessToken = (String) tok.get("access_token");
        Number expiresIn = (Number) tok.get("expires_in");
        String refreshToken = (String) tok.get("refresh_token"); // suele venir solo la 1ª vez
        String scope = (String) tok.get("scope");

        GoogleOAuthToken entity = repo.findById(userId).orElse(new GoogleOAuthToken(userId));

        if (refreshToken != null && !refreshToken.isBlank()) {
            entity.setRefreshToken(refreshToken);
        }

        entity.setAccessToken(accessToken);
        entity.setAccessTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn.longValue() : 3600));
        entity.setScope(scope);
        entity.setUpdatedAt(Instant.now());

        repo.save(entity);
    }

    @Transactional
    public String getValidAccessTokenOrRefresh(String userId) {
        GoogleOAuthToken entity = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("NO_GOOGLE_SESSION: el usuario no autorizó Google aún"));

        Instant now = Instant.now();
        if (entity.getAccessToken() != null && entity.getAccessTokenExpiresAt() != null
                && now.isBefore(entity.getAccessTokenExpiresAt().minusSeconds(120))) {
            return entity.getAccessToken();
        }

        return forceRefreshAccessToken(userId);
    }

    @Transactional
    public String forceRefreshAccessToken(String userId) {
        GoogleOAuthToken entity = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("NO_GOOGLE_SESSION"));

        if (entity.getRefreshToken() == null || entity.getRefreshToken().isBlank()) {
            throw new RuntimeException("NO_REFRESH_TOKEN: falta consentimiento offline (primera autorización)");
        }

        Map<String, Object> refreshed = tokenClient.refreshAccessToken(entity.getRefreshToken());
        String newAccess = (String) refreshed.get("access_token");
        Number expiresIn = (Number) refreshed.get("expires_in");

        entity.setAccessToken(newAccess);
        entity.setAccessTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn.longValue() : 3600));
        entity.setUpdatedAt(Instant.now());
        repo.save(entity);

        return newAccess;
    }
}

