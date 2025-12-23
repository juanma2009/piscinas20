package com.bolsadeideas.springboot.app;

import com.bolsadeideas.springboot.app.models.entity.GoogleOAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleOAuthConfig {
}
