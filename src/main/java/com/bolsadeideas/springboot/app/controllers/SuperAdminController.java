package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.*;
import com.bolsadeideas.springboot.app.models.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private GastoAppRepository gastoAppRepository;

    @Autowired
    private IngresoEmpresaRepository ingresoEmpresaRepository;

    @Autowired
    private IncidenciaAppRepository incidenciaAppRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        long totalEmpresas = empresaRepository.count();
        // Sumar ingresos y gastos (esto se podría hacer con @Query en repositorios para mayor eficiencia)
        double totalIngresos = 0;
        for (IngresoEmpresa i : ingresoEmpresaRepository.findAll()) totalIngresos += i.getImporte();

        double totalGastos = 0;
        for (GastoApp g : gastoAppRepository.findAll()) totalGastos += g.getImporte();

        long incidenciasPendientes = incidenciaAppRepository.findByEstado("PENDIENTE").size();

        model.addAttribute("titulo", "Dashboard Super Admin");
        model.addAttribute("totalEmpresas", totalEmpresas);
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("totalGastos", totalGastos);
        model.addAttribute("incidenciasPendientes", incidenciasPendientes);
        
        return "superadmin/dashboard";
    }

    // --- GESTIÓN DE EMPRESAS ---
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

    // --- GESTIÓN DE INGRESOS ---
    @GetMapping("/ingresos")
    public String listarIngresos(Model model) {
        model.addAttribute("titulo", "Control de Ingresos por Cliente");
        model.addAttribute("ingresos", ingresoEmpresaRepository.findAll());
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("nuevoIngreso", new IngresoEmpresa());
        return "superadmin/ingresos";
    }

    @PostMapping("/ingresos/guardar")
    public String guardarIngreso(IngresoEmpresa ingreso, @RequestParam("empresaId") Long empresaId, RedirectAttributes flash) {
        Empresa e = empresaRepository.findById(empresaId).orElse(null);
        if (e != null) {
            ingreso.setEmpresa(e);
            ingresoEmpresaRepository.save(ingreso);
            flash.addFlashAttribute("success", "Pago registrado correctamente.");
        }
        return "redirect:/superadmin/ingresos";
    }

    // --- GESTIÓN DE GASTOS ---
    @GetMapping("/gastos")
    public String listarGastos(Model model) {
        model.addAttribute("titulo", "Control de Gastos (Servidor/Dominio)");
        model.addAttribute("gastos", gastoAppRepository.findAll());
        model.addAttribute("nuevoGasto", new GastoApp());
        return "superadmin/gastos";
    }

    @PostMapping("/gastos/guardar")
    public String guardarGasto(GastoApp gasto, RedirectAttributes flash) {
        gastoAppRepository.save(gasto);
        flash.addFlashAttribute("success", "Gasto registrado correctamente.");
        return "redirect:/superadmin/gastos";
    }

    // --- GESTIÓN DE INCIDENCIAS ---
    @GetMapping("/incidencias")
    public String listarIncidencias(Model model) {
        model.addAttribute("titulo", "Gestión de Incidencias");
        model.addAttribute("incidencias", incidenciaAppRepository.findAll());
        return "superadmin/incidencias";
    }

    @PostMapping("/incidencias/cambiar-estado")
    public String cambiarEstadoIncidencia(@RequestParam Long id, @RequestParam String estado, RedirectAttributes flash) {
        IncidenciaApp inc = incidenciaAppRepository.findById(id).orElse(null);
        if (inc != null) {
            inc.setEstado(estado);
            incidenciaAppRepository.save(inc);
            flash.addFlashAttribute("success", "Estado de incidencia actualizado.");
        }
        return "redirect:/superadmin/incidencias";
    }
}
