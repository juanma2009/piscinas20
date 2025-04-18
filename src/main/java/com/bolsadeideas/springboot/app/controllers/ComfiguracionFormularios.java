package com.bolsadeideas.springboot.app.controllers;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/configuracion")
public class ComfiguracionFormularios {

@Autowired
    private IServicioService servicioService;
    private IMetalService metalService;
    private ISubgrupoService subgrupoService;
    private IEstadoService estadoService;


    @GetMapping
    @RequestMapping("/configuracion")
    public String mostrarConfiguracion(Model model) {
        // Cargamos en el model las listas actuales para cada sección
        model.addAttribute("servicios", servicioService.findAll());
        model.addAttribute("metales",    metalService.findAll());
        model.addAttribute("subgrupos",  subgrupoService.findAll());
        model.addAttribute("estados",    estadoService.findAll());
        return "configuracion/configuracion";
        // Thymeleaf buscará src/main/resources/templates/configuracion/configuracion.html
    }


}
