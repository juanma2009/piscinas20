package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.*;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/citas")
@SessionAttributes("cita")
@Secured("ROLE_ADMIN")
public class CitaController {

    @Autowired
    private ICitaService citaService;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo", "Listado de Citas");
        model.addAttribute("citas", citaService.findAllWithEntities());
        return "citas/listar";
    }

    @GetMapping("/form")
    public String crear(Map<String, Object> model) {
        Cita cita = new Cita();
        model.put("cita", cita);
        model.put("titulo", "Programar Cita");
        model.put("clientes", clienteService.findAll());
        model.put("tipos", Cita.TipoCita.values());
        model.put("estados", Cita.EstadoCita.values());
        return "citas/form";
    }

    @GetMapping("/form/{clienteId}")
    public String crearConCliente(@PathVariable(value = "clienteId") Long clienteId, Map<String, Object> model, RedirectAttributes flash) {
        Cliente cliente = clienteService.findOne(clienteId);
        if (cliente == null) {
            flash.addFlashAttribute("error", "El cliente no existe");
            return "redirect:/listar";
        }
        Cita cita = new Cita();
        cita.setCliente(cliente);
        model.put("cita", cita);
        model.put("clienteId", clienteId);
        model.put("titulo", "Programar Cita para: " + cliente.getNombre());
        model.put("tipos", Cita.TipoCita.values());
        model.put("estados", Cita.EstadoCita.values());
        return "citas/form";
    }

    @GetMapping("/form/pedido/{pedidoId}")
    public String crearConPedido(@PathVariable(value = "pedidoId") Long pedidoId, Map<String, Object> model, RedirectAttributes flash) {
        Pedido pedido = pedidoService.findOne(pedidoId);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos/listarPedidos";
        }
        Cita cita = new Cita();
        cita.setPedido(pedido);
        cita.setCliente(pedido.getCliente());
        cita.setTipo(Cita.TipoCita.RECOGIDA_PEDIDO);
        
        model.put("cita", cita);
        model.put("clienteId", pedido.getCliente().getId());
        model.put("titulo", "Programar Recogida para Pedido #" + pedidoId);
        model.put("tipos", Cita.TipoCita.values());
        model.put("estados", Cita.EstadoCita.values());
        return "citas/form";
    }

    @GetMapping("/form/editar/{id}")
    public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {
        Cita cita = null;
        if (id > 0) {
            cita = citaService.findByIdWithEntities(id);
            if (cita == null) {
                flash.addFlashAttribute("error", "La cita no existe en la base de datos");
                return "redirect:/citas/listar";
            }
        } else {
            flash.addFlashAttribute("error", "El ID de la cita no puede ser cero");
            return "redirect:/citas/listar";
        }
        model.put("cita", cita);
        if (cita.getCliente() != null) {
            model.put("clienteId", cita.getCliente().getId());
        }
        model.put("titulo", "Editar Cita");
        model.put("tipos", Cita.TipoCita.values());
        model.put("estados", Cita.EstadoCita.values());
        model.put("historial", citaService.getHistorialByCitaId(id));
        return "citas/form";
    }

    @PostMapping("/form")
    public String guardar(@Valid Cita cita, BindingResult result, Model model, RedirectAttributes flash, SessionStatus status, Principal principal) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Programar Cita");
            model.addAttribute("clientes", clienteService.findAll());
            model.addAttribute("tipos", Cita.TipoCita.values());
            model.addAttribute("estados", Cita.EstadoCita.values());
            if (cita.getId() != null) {
                model.addAttribute("historial", citaService.getHistorialByCitaId(cita.getId()));
            }
            return "citas/form";
        }

        String mensajeFlash = (cita.getId() != null) ? "Cita editada con éxito!" : "Cita programada con éxito!";
        String username = (principal != null) ? principal.getName() : "Sistema";
        Cita citaGuardada = citaService.save(cita, username);
        status.setComplete();
        
        try {
            emailService.enviarNotificacionCita(citaGuardada);
        } catch (Exception e) {
            System.err.println("Error al enviar email de notificación: " + e.getMessage());
        }
        
        try {
            googleCalendarService.crearEventoCita(citaGuardada, username);
        } catch (Exception e) {
            System.err.println("Error al crear evento en Google Calendar: " + e.getMessage());
        }
        
        flash.addFlashAttribute("success", mensajeFlash);
        return "redirect:/citas/listar";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable(value = "id") Long id, 
                           @RequestParam(value = "clienteId", required = false) Long clienteId,
                           RedirectAttributes flash) {
        if (id > 0) {
            citaService.delete(id);
            flash.addFlashAttribute("success", "Cita eliminada con éxito!");
        }
        
        if (clienteId != null) {
            return "redirect:/ver/" + clienteId;
        }
        return "redirect:/citas/listar";
    }
}
