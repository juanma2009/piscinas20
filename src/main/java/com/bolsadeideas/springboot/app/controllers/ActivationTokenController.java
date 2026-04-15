package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.ActivationTokenRepository;
import com.bolsadeideas.springboot.app.models.dto.PasswordActivationDTO;
import com.bolsadeideas.springboot.app.models.entity.ActivationToken;
import com.bolsadeideas.springboot.app.models.entity.User;
import com.bolsadeideas.springboot.app.models.entity.UserStatus;
import com.bolsadeideas.springboot.app.models.service.UserService;
import com.bolsadeideas.springboot.app.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class ActivationTokenController {

    @Autowired
    private ActivationTokenRepository tokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/validar-activacion")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        String hash = SecurityUtils.hashToken(token);
        Optional<ActivationToken> dbToken = tokenRepository.findByTokenHash(hash);

        if (dbToken.isEmpty() || dbToken.get().isUsed()) {
            return ResponseEntity.status(400).body(Map.of("message", "El enlace ya no es válido o ya fue utilizado. Solicita al administrador que reenvíe la invitación."));
        }

        if (dbToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(Map.of("message", "El enlace ha caducado por superar las 24 horas permitidas. Solicita un nuevo enlace."));
        }

        return ResponseEntity.ok(Map.of("valid", true, "username", dbToken.get().getUser().getUsername()));
    }

    @PostMapping("/activar-perfil")
    @Transactional
    public ResponseEntity<?> activateAndSetPassword(@RequestBody PasswordActivationDTO dto) {
        String hash = SecurityUtils.hashToken(dto.getToken());
        ActivationToken dbToken = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (dbToken.isUsed() || dbToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "El enlace ha caducado o ya ha sido usado."));
        }

        User user = dbToken.getUser();

        // 1. Guardar contraseña nueva con BCrypt
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        // 2. Transitar a estado ACTIVO
        user.setStatus(UserStatus.ACTIVO);
        user.setActive(true);

        userService.save(user);

        // 3. Invalidar el Token marcándolo como usado para evitar Replay Attacks
        dbToken.setUsed(true);
        tokenRepository.save(dbToken);

        return ResponseEntity.ok(Map.of("message", "Tu cuenta se activó correctamente. Se guardó tu nueva contraseña de forma segura."));
    }
}
