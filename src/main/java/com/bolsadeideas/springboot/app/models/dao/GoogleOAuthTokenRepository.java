package com.bolsadeideas.springboot.app.models.dao;

import com.bolsadeideas.springboot.app.models.entity.GoogleOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, String> {
}
