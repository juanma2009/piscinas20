package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.RoleRepository;
import com.bolsadeideas.springboot.app.models.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Controller
@RequestMapping("/admin/roles")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String mostrarRoles(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("role", new Role());
        return "roles/roles";
    }

    @PostMapping
    public String crearRol(@Valid @ModelAttribute("role") Role role, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            return "roles/roles";
        }
        roleRepository.save(role);
        model.addAttribute("successMessage", "Rol creado exitosamente.");
        return "redirect:/admin/roles";
    }

    @PostMapping("/update")
    public String actualizarRol(@RequestParam Long id,
                                @RequestParam String nombre,
                                @RequestParam String descripcion,
                                @RequestParam(defaultValue = "false") boolean estado) {
        Role rol = roleRepository.findById(id).orElse(null);
        if (rol != null) {
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rol.setEstado(estado);
            roleRepository.save(rol);
        }
        //despues de actualizar el rol, redirigir a la lista de roles
        return "redirect:/admin/roles";
    }

    @PostMapping("/delete")
    public String eliminarRol(@RequestParam Long id) {
        roleRepository.deleteById(id);
        //despues de eliminar el rol, redirigir a la lista de roles
        return "redirect:/admin/roles";
    }
}
