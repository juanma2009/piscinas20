package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.EmpresaRepository;
import com.bolsadeideas.springboot.app.models.entity.Empresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        long totalEmpresas = empresaRepository.count();
        model.addAttribute("titulo", "Dashboard Super Admin");
        model.addAttribute("totalEmpresas", totalEmpresas);
        
        return "superadmin/dashboard";
    }

    @GetMapping("/empresas")
    public String empresas(Model model) {
        Iterable<Empresa> empresas = empresaRepository.findAll();
        model.addAttribute("titulo", "Gestión de Empresas / Clientes");
        model.addAttribute("empresas", empresas);
        return "superadmin/empresas";
    }

    @PostMapping("/empresas/toggle-status")
    public String toggleStatusEmpresa(@RequestParam("id") Long id, RedirectAttributes flash) {
        Empresa empresa = empresaRepository.findById(id).orElse(null);
        if (empresa != null) {
            empresa.setActivo(!empresa.isActivo());
            empresaRepository.save(empresa);
            flash.addFlashAttribute("success", "Estado de la empresa actualizado correctamente.");
        } else {
            flash.addFlashAttribute("error", "No se encontró la empresa.");
        }
        return "redirect:/superadmin/empresas";
    }
}
