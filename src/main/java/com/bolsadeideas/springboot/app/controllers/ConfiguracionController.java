package com.bolsadeideas.springboot.app.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionController {

    @GetMapping
    public String mostrarConfiguracion() {
        // Thymeleaf buscará src/main/resources/templates/configuracion/configuracion.html
        return "configuracion/configuracion";
    }
}
