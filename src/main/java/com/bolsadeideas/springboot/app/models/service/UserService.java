package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.UserRepository;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);
        // Compara la contraseña encriptada usando BCrypt
        return user != null && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void registerUser(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setActive(true);
        userRepository.save(user);
    }

    public void updateUser(User user) {
        // Solo encripta si la contraseña fue proporcionada
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }
        userRepository.save(user);
    }

    // Método para verificar si el usuario ya existe
    public boolean userExists(String username) {
        return userRepository.findByUsername(username) != null;
    }


    public void deleteUserById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id); // Elimina el usuario por su ID
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setActive(false); // Cambia el estado a inactivo
        userRepository.save(user); // Guarda los cambios
    }

    public boolean isUserInactive(String username) {
        User user = userRepository.findByUsername(username);
        return user != null && !user.isActive();
    }

    public boolean findByUsernameisActive(String username) {
        User user = userRepository.findByUsernameisActive(username);
        return user != null && !user.isActive();
    }

    //comprobar si esta enable authenticator
    public boolean isTwoFactorEnabled(String username) {
        User user = userRepository.findByUsername(username);
        return user != null && user.isTwoFactorEnabled();
    }

    //obtner el email del usuario autenticado
    public String getUserEmail(String username) {
        User user = userRepository.findByUsername(username);
        return user != null ? user.getEmail() : null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void save(User user) {
        userRepository.save(user);
    }}




