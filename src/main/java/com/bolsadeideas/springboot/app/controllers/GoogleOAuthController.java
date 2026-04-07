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

    /**
     * Devuelve el email de la cuenta de Google vinculada si existe.
     * Útil para detectar qué cuenta estamos usando.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Principal principal) {
        Map<String, Object> info = oauthService.getLinkedAccountInfo(principal.getName());
        if (info == null) {
            return ResponseEntity.ok(Map.of("linked", false));
        }
        
        // Si el mapa ya tiene el flag 'linked' es que viene de un catch/error en el service
        boolean isLinked = info.containsKey("linked") ? (Boolean) info.get("linked") : true;

        return ResponseEntity.ok(Map.of(
                "linked", isLinked,
                "email", info.getOrDefault("email", "unknown"),
                "name", info.getOrDefault("name", "unknown"),
                "picture", info.getOrDefault("picture", "")
        ));
    }

    /**
     * Permite al usuario desvincular su cuenta actual para entrar con otra.
     */
    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(HttpServletRequest request, Principal principal) {
        String xrw = request.getHeader("X-Requested-With");
        if (xrw == null || !xrw.equalsIgnoreCase("XmlHttpRequest")) {
            return ResponseEntity.status(403).body(Map.of("error", "CSRF_CHECK_FAILED"));
        }
        oauthService.disconnect(principal.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }
}



