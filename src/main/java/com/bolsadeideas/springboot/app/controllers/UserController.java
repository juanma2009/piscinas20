package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Role;
import com.bolsadeideas.springboot.app.models.entity.User;
import com.bolsadeideas.springboot.app.models.dao.RoleRepository;
import com.bolsadeideas.springboot.app.models.dao.UserRepository;
import com.bolsadeideas.springboot.app.models.service.UserService;
import com.bolsadeideas.springboot.app.util.paginator.PageRender;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class UserController {

    Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder; // Define un bean en tu config de seguridad


    /**
     * @param model
     * @return
     */
    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        User user = new User();
        Set<Role> rolesSet = new HashSet<>(roleRepository.findAll()); // Convertir a Set
        log.info("Roles: " + rolesSet);
        model.addAttribute("titulo", "Crear Usuarios");
        model.addAttribute("user", user);
        model.addAttribute("rolesList", rolesSet);
        return "user/create_user_with_role";
    }

    /**
     * Método para registrar un usuario con roles
     *
     * @param user
     * @param model
     * @param roles
     * @return
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model, @RequestParam List<Long> roles) {
        // Verificar si el nombre de usuario ya existe
        if (userService.userExists(user.getUsername())) {
            log.info("El usuario ya existe.");
            model.addAttribute("errorMessage", "El usuario ya existe.");

            // Recargar la lista de roles para la vista
            List<Role> rolesList = roleRepository.findAll();
            model.addAttribute("rolesList", rolesList);

            return "user/create_user_with_role";
        }

        // Verificar si el email ya existe
        if (userRepository.existsByEmail(user.getEmail())) {
            log.info("El email ya existe.");
            model.addAttribute("errorMessage", "El email ya existe.");

            // Recargar la lista de roles para la vista
            List<Role> rolesList = roleRepository.findAll();
            model.addAttribute("rolesList", rolesList);

            return "user/create_user_with_role";
        }

        // Obtener los roles correspondientes y convertirlos a un Set

        Set<Role> userRoles = new HashSet<>(roleRepository.findAllById(roles));
        user.setRoles(userRoles); // Asigna el Set de roles al usuario

        // Registrar el usuario
        userService.registerUser(user); // Aquí puedes incluir la lógica de codificación y guardado del usuario

        return "redirect:/users";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(name = "page", defaultValue = "0") int page, Principal principal, Model model) {
        Pageable pageRequest = PageRequest.of(page, 6); // Cambia 4 por el tamaño de página deseado
        Page<User> users = userRepository.findActiveUsers(pageRequest); // Página de usuarios
        PageRender<User> pageRender = new PageRender<>("/users", users); // Usamos el paginador PageRender

        String currentUsername = principal.getName();
        User currentUser = userRepository.findByUsername(currentUsername);
        boolean isAdmin = currentUser != null && currentUser.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getNombre()) || "ROLE_ADMIN".equals(role.getNombre()));

        model.addAttribute("users", users.getContent()); // Lista de usuarios
        model.addAttribute("page", pageRender); // Paginación
        model.addAttribute("titulo", "Lista de Usuarios"); // Título de la vista
        model.addAttribute("isAdmin", isAdmin);

        return "user/list_users"; // Retornamos la vista
    }

    @PostMapping("/users/deactivate/{id}")
    public String deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return "user/list_users"; // Redirige a la lista de usuarios
    }

    @GetMapping("/user/profile")
    public String showProfile(@RequestParam(required = false) Long userId, Principal principal, Model model) {
        String currentUsername = principal.getName();
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            return "redirect:/login";
        }

        User userToEdit;
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getNombre()) || "ROLE_ADMIN".equals(role.getNombre()));

        if (userId != null) {
            userToEdit = userRepository.findById(userId).orElse(null);
            if (userToEdit == null) {
                model.addAttribute("errorMessage", "Usuario no encontrado.");
                return "redirect:/users";
            }
        } else {
            userToEdit = currentUser;
        }

        Set<Role> rolesSet = new HashSet<>(roleRepository.findAll());
        model.addAttribute("user", userToEdit);
        model.addAttribute("rolesList", rolesSet);
        model.addAttribute("titulo", userId != null ? "Editar Usuario" : "Perfil del Usuario");
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("editingOwnProfile", userToEdit.getId().equals(currentUser.getId()));
        return "user/profile";
    }

    @PostMapping("/user/updateProfile")
    public String updateProfile(@ModelAttribute("user") User updatedUser, @RequestParam(required = false) Long userId, Principal principal, Model model) {
        String currentUsername = principal.getName();
        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getNombre()) || "ROLE_ADMIN".equals(role.getNombre()));

        User userToEdit;
        if (userId != null) {
            userToEdit = userRepository.findById(userId).orElse(null);
            if (userToEdit == null) {
                model.addAttribute("errorMessage", "Usuario no encontrado.");
                return "redirect:/users";
            }
        } else {
            userToEdit = currentUser;
        }

        // Check if username is being changed and if it already exists (only if different user)
        if (!userToEdit.getUsername().equals(updatedUser.getUsername()) && userService.userExists(updatedUser.getUsername())) {
            model.addAttribute("errorMessage", "El username ya existe.");
            Set<Role> rolesSet = new HashSet<>(roleRepository.findAll());
            model.addAttribute("user", userToEdit);
            model.addAttribute("rolesList", rolesSet);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("editingOwnProfile", userToEdit.getId().equals(currentUser.getId()));
            return "user/profile";
        }

        // Check if email is being changed and if it already exists
        if (!userToEdit.getEmail().equals(updatedUser.getEmail()) && userRepository.existsByEmail(updatedUser.getEmail())) {
            model.addAttribute("errorMessage", "El email ya existe.");
            Set<Role> rolesSet = new HashSet<>(roleRepository.findAll());
            model.addAttribute("user", userToEdit);
            model.addAttribute("rolesList", rolesSet);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("editingOwnProfile", userToEdit.getId().equals(currentUser.getId()));
            return "user/profile";
        }

        // Update fields
        userToEdit.setUsername(updatedUser.getUsername());
        userToEdit.setEmail(updatedUser.getEmail());
        userToEdit.setNombre(updatedUser.getNombre());
        userToEdit.setApellido(updatedUser.getApellido());
        if (isAdmin) {
            userToEdit.setRoles(updatedUser.getRoles());
            userToEdit.setTwoFactorEnabled(updatedUser.isTwoFactorEnabled());
        }

        // If password is provided, update it
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            userService.updateUser(userToEdit);
        } else {
            userRepository.save(userToEdit);
        }

        Set<Role> rolesSet = new HashSet<>(roleRepository.findAll());
        model.addAttribute("user", userToEdit);
        model.addAttribute("rolesList", rolesSet);
        model.addAttribute("titulo", userId != null ? "Editar Usuario" : "Perfil del Usuario");
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("editingOwnProfile", userToEdit.getId().equals(currentUser.getId()));
        model.addAttribute("successMessage", "Perfil actualizado correctamente");
        return "user/profile";
    }

    // Cambiar contraseña vía AJAX
    @PostMapping("/user/changePasswordAjax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changePasswordAjax(
            @RequestParam(required = false) String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) Long userId,
            Principal principal) {

        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Usuario no autenticado"));
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getNombre()) || "ROLE_ADMIN".equals(role.getNombre()));

        User userToChange;
        if (userId != null && isAdmin) {
            userToChange = userRepository.findById(userId).orElse(null);
            if (userToChange == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "Usuario no encontrado"));
            }
        } else {
            userToChange = currentUser;
        }

        if (userToChange.getId().equals(currentUser.getId()) &&
                (currentPassword == null || !passwordEncoder.matches(currentPassword, currentUser.getPassword()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Contraseña actual incorrecta"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Las contraseñas no coinciden"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "La contraseña debe tener al menos 6 caracteres"));
        }

        if (userToChange.getId().equals(currentUser.getId()) && passwordEncoder.matches(newPassword, currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "La nueva contraseña no puede ser igual a la actual"));
        }

        userToChange.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userToChange);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Contraseña cambiada correctamente"));
    }

    @PostMapping("/user/verifyCurrentPassword")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verifyCurrentPassword(
            @RequestParam String currentPassword,
            Principal principal) {
        boolean isValid = userService.authenticate(principal.getName(), currentPassword);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    private void prepareProfileModel(Model model, User user) {
        Set<Role> rolesSet = new HashSet<>(roleRepository.findAll());
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getNombre()) || "ROLE_ADMIN".equals(r.getNombre()));

        model.addAttribute("user", user);
        model.addAttribute("rolesList", rolesSet);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("editingOwnProfile", true);
        model.addAttribute("titulo", "Perfil del Usuario");
    }

    @PostMapping("/user/changePassword")
    public String changePassword(@RequestParam(required = false) String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @RequestParam(required = false) Long userId,
                                 Principal principal, Model model) {

        String currentUsername = principal.getName();
        User currentUser = userService.findByUsername(currentUsername);

        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getNombre()) || "ROLE_ADMIN".equals(role.getNombre()));

        User userToChange = (userId != null && isAdmin) ?
                userService.findByUsername(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuario no encontrado")).getUsername()) :
                currentUser;

        // Validar contraseña actual solo si el propio usuario cambia
        if (userToChange.getId().equals(currentUser.getId()) && !userService.authenticate(currentUsername, currentPassword)) {
            model.addAttribute("errorMessage", "Contraseña actual incorrecta");
            prepareProfileModel(model, currentUser);
            return "user/profile";
        }

        // Validar nueva contraseña
        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("errorMessage", "La nueva contraseña debe tener al menos 6 caracteres");
            prepareProfileModel(model, currentUser);
            return "user/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "La nueva contraseña y la confirmación no coinciden");
            prepareProfileModel(model, currentUser);
            return "user/profile";
        }

        userToChange.setPassword(newPassword); // El service se encargará de codificar
        userService.updateUser(userToChange);

        return "redirect:/users"; // o redirigir al perfil según prefieras
    }




}

