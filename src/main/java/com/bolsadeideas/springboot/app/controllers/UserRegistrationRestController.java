package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.ActivationTokenRepository;
import com.bolsadeideas.springboot.app.models.dao.RoleRepository;
import com.bolsadeideas.springboot.app.models.dto.PasswordActivationDTO;
import com.bolsadeideas.springboot.app.models.dto.UserCreationDTO;
import com.bolsadeideas.springboot.app.models.entity.ActivationToken;
import com.bolsadeideas.springboot.app.models.entity.Role;
import com.bolsadeideas.springboot.app.models.entity.User;
import com.bolsadeideas.springboot.app.models.entity.UserStatus;
import com.bolsadeideas.springboot.app.models.service.EmailService; // Force recompile
import com.bolsadeideas.springboot.app.models.service.UserService;
import com.bolsadeideas.springboot.app.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import java.util.UUID;


@RestController
@RequestMapping("/api/users")
public class UserRegistrationRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ActivationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/crear-pendiente")
    @Transactional
    public ResponseEntity<?> createUserPending(@RequestBody UserCreationDTO dto) {

        if (userService.findByUsername(dto.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre de usuario ya está registrado en el sistema."));
        }



        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setNombre(dto.getNombre());
        newUser.setApellido(dto.getApellido());
        newUser.setPuesto(dto.getPuesto());
        newUser.setTwoFactorEnabled(dto.isTwoFactorEnabled());

        newUser.setStatus(UserStatus.PENDIENTE_ACTIVACION);
        newUser.setActive(false); 

        // Generar una contraseña UUID, la real se establecerá luego
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Asignar roles seleccionados de DB
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            Iterable<Role> roles = roleRepository.findAllById(dto.getRoles());
            for(Role role : roles) {
               newUser.addRole(role);
            }
        }

        User savedUser = userService.save(newUser);

        String plainToken = SecurityUtils.generateSecureToken();
        ActivationToken tokenEnt = new ActivationToken();
        tokenEnt.setTokenHash(SecurityUtils.hashToken(plainToken));
        tokenEnt.setUser(savedUser);
        tokenEnt.setExpiryDate(LocalDateTime.now().plusHours(24));
        tokenRepository.save(tokenEnt);

        emailService.enviarEmailActivacion(savedUser.getEmail(), savedUser.getNombre(), plainToken);

        return ResponseEntity.ok(Map.of("message", "Usuario creado como PENDIENTE de activación."));
    }
}
