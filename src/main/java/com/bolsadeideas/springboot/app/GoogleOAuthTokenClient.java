package com.bolsadeideas.springboot.app;

import com.bolsadeideas.springboot.app.models.entity.GoogleOAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
public class GoogleOAuthTokenClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthTokenClient.class);

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties props;

    public GoogleOAuthTokenClient(GoogleOAuthProperties props, RestTemplateBuilder builder) {
        this.props = props;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * OAuth2 code -> tokens (server-side exchange)
     * Para GIS initCodeClient (popup), usa redirect_uri=postmessage.
     */
    public Map<String, Object> exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("redirect_uri", "postmessage");
        form.add("grant_type", "authorization_code");

        // Si quieres, puedes incluir client_id también (con BasicAuth ya basta)
        form.add("client_id", props.getClientId());

        return postForm(form);
    }

    public Map<String, Object> refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        form.add("client_id", props.getClientId());

        return postForm(form);
    }

    private Map<String, Object> postForm(MultiValueMap<String, String> form) {
        String tokenUri = sanitizeUri(props.getTokenUri());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Recomendado: autenticar el cliente con Basic Auth
        // (evitas líos por comillas/espacios en el secret en el body)
        headers.setBasicAuth(props.getClientId(), props.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUri, req, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("TOKEN_ENDPOINT_ERROR: " + resp.getStatusCode());
            }

            //noinspection unchecked
            return (Map<String, Object>) resp.getBody();

        } catch (HttpClientErrorException e) {
            // B) Log útil (Google suele devolver JSON con error/error_description)
            String body = e.getResponseBodyAsString();
            log.error("❌ Google token endpoint error. HTTP {} {}", e.getStatusCode().value(), e.getStatusText());
            log.error("❌ tokenUri={}", tokenUri);
            log.error("❌ body={}", body);

            // Tip: si body viene vacío, aún así sabes que es invalid_client casi siempre (401)
            throw new RuntimeException("TOKEN_ENDPOINT_HTTP_" + e.getStatusCode().value() + ": " + body, e);
        }
    }

    private String sanitizeUri(String uri) {
        if (uri == null) return null;
        return uri.replace("\"", "").trim();
    }
}

