package com.bolsadeideas.springboot.app.models.entity;


import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "google_drive_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class GoogleDriveToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;  // Normalmente el login/email del usuario en tu app

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // Fecha/hora de expiración del access_token

    @Column(name = "scope", length = 500)
    private String scope;  // Scopes concedidos (opcional)

    @Column(name = "token_type", length = 50)
    private String tokenType;  // Normalmente "Bearer"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ====================
    // CONSTRUCTORES
    // ====================

    public GoogleDriveToken() {
        this.createdAt = LocalDateTime.now();
    }

    public GoogleDriveToken(String userId, String refreshToken) {
        this();
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.updatedAt = LocalDateTime.now();
    }

    // ====================
    // GETTERS Y SETTERS
    // ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ====================
    // MÉTODOS AUXILIARES
    // ====================

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "GoogleDriveToken{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", accessToken='[PROTECTED]'" +
                ", refreshToken='[PROTECTED]'" +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}