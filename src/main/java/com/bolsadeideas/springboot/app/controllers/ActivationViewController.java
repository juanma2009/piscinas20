package com.bolsadeideas.springboot.app.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActivationViewController {

    @GetMapping("/activar-cuenta")
    public String showActivationPage() {
        // Devuelve el nombre del template "activar_cuenta.html"
        return "activar_cuenta";
    }
}
