package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.service.GoogleOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/google/oauth")
public class GoogleOAuthController {

    private final GoogleOAuthService oauthService;

    public GoogleOAuthController(GoogleOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @PostMapping("/code")
    public ResponseEntity<?> receiveCode(
            @RequestParam("code") String code,
            // @RequestParam(value="redirectUri", required=false) String redirectUri, // opcional si el FE aún lo manda
            HttpServletRequest request,
            Principal principal
    ) {
        String xrw = request.getHeader("X-Requested-With");
        if (xrw == null || !xrw.equalsIgnoreCase("XmlHttpRequest")) {
            return ResponseEntity.status(403).body(Map.of("error", "CSRF_CHECK_FAILED"));
        }

        String userId = principal.getName();
        oauthService.storeFromAuthorizationCode(userId, code);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/access-token")
    public ResponseEntity<?> getAccessToken(Principal principal) {
        String token = oauthService.getValidAccessTokenOrRefresh(principal.getName());
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}



