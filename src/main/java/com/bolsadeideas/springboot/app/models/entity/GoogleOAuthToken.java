package com.bolsadeideas.springboot.app.models.entity;



import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Data
@Entity
@Table(name = "google_oauth_token")
public class GoogleOAuthToken {

    @Id
    @Column(name = "user_id", nullable = false, length = 150)
    private String userId;

    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    @Column(name = "access_token", length = 2000)
    private String accessToken;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "scope", length = 1000)
    private String scope;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public GoogleOAuthToken() {}

    public GoogleOAuthToken(String userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }


}

