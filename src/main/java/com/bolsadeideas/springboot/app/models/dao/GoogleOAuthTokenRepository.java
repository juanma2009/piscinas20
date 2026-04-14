package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.GoogleDriveToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleDriveToken, Long> {
    Optional<GoogleDriveToken> findByUserId(String userId);
}
