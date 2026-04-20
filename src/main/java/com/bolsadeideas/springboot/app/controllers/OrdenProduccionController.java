package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.CompraInventarioRepository;
import com.bolsadeideas.springboot.app.models.dao.IProductoDao;
import com.bolsadeideas.springboot.app.models.dao.OrdenProduccionRepository;
import com.bolsadeideas.springboot.app.models.entity.CompraInventario;
import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.entity.Producto;
import com.bolsadeideas.springboot.app.models.service.InventarioJoyeriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador de Órdenes de Producción para el taller de joyería.
 * Gestiona: abrir orden, cerrar orden (con balance de masa), historial y trazabilidad.
 */
@Controller
@RequestMapping("/produccion")
@Secured("ROLE_ADMIN")
public class OrdenProduccionController {

    @Autowired private InventarioJoyeriaService joyeriaService;
    @Autowired private OrdenProduccionRepository ordenRepo;
    @Autowired private CompraInventarioRepository compraRepo;
    @Autowired private IProductoDao productoDao;
    @Autowired private com.bolsadeideas.springboot.app.models.dao.PedidoDao pedidoDao;

    // ══════════════════════════════════════════════════════════════════════════
    //  LISTADO DE ÓRDENES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("ordenesEnProceso",
                ordenRepo.findByEstadoOrderByFechaInicioDesc(OrdenProduccion.EstadoOrden.EN_PROCESO));
        model.addAttribute("ordenesTerminadas",
                ordenRepo.findTop20ByOrderByFechaInicioDesc().stream()
                        .filter(o -> o.getEstado() == OrdenProduccion.EstadoOrden.TERMINADO)
                        .collect(Collectors.toList()));
        model.addAttribute("titulo", "Órdenes de Producción");
        return "produccion/listar";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ABRIR ORDEN — formulario GET
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/nueva")
    public String nuevaOrdenForm(
            @RequestParam(required = false) Long pedidoId,
            Model model) {

        // Solo lotes con peso disponible
        List<CompraInventario> lotesDisponibles = compraRepo.findTop50ByOrderByFechaCompraDesc()
                .stream()
                .filter(c -> c.getCodigoLote() != null
                          && c.getPesoActualGr() != null
                          && c.getPesoActualGr() > 0)
                .collect(Collectors.toList());

        model.addAttribute("lotesDisponibles", lotesDisponibles);
        
        if (pedidoId != null) {
            Pedido pedido = pedidoDao.findById(pedidoId).orElse(null);
            model.addAttribute("pedido", pedido);
        }
        
        model.addAttribute("pedidoIdParam", pedidoId);
        model.addAttribute("titulo", "Abrir Orden de Producción");
        return "produccion/formOrden";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ABRIR ORDEN — POST guardar
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/nueva")
    public String abrirOrden(
            @RequestParam(required = false) Long pedidoId,
            @RequestParam String descripcion,
            @RequestParam List<Long> compraIds,
            @RequestParam List<Double> pesosGr,
            Principal principal,
            RedirectAttributes flash) {

        if (compraIds.size() != pesosGr.size()) {
            flash.addFlashAttribute("error", "Número de lotes y pesos no coinciden");
            return "redirect:/produccion/nueva";
        }

        // Construir mapa compraId → pesoGr
        Map<Long, Double> lotesYPesos = new LinkedHashMap<>();
        for (int i = 0; i < compraIds.size(); i++) {
            if (compraIds.get(i) != null && pesosGr.get(i) != null && pesosGr.get(i) > 0) {
                lotesYPesos.put(compraIds.get(i), pesosGr.get(i));
            }
        }

        if (lotesYPesos.isEmpty()) {
            flash.addFlashAttribute("error", "Debes indicar al menos un lote con peso mayor que 0");
            return "redirect:/produccion/nueva";
        }

        try {
            OrdenProduccion orden = joyeriaService.abrirOrdenProduccion(
                    pedidoId, descripcion, lotesYPesos,
                    principal != null ? principal.getName() : "Sistema");

            flash.addFlashAttribute("success",
                "Orden #" + orden.getId() + " abierta. " +
                "Total material en proceso: " + orden.getPesoEntradaGr() + "g");
            return "redirect:/produccion/" + orden.getId() + "/cerrar";
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/produccion/nueva";
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CERRAR ORDEN — formulario GET
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}/cerrar")
    public String cerrarOrdenForm(@PathVariable Long id, Model model) {
        OrdenProduccion orden = ordenRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + id));

        if (orden.getEstado() == OrdenProduccion.EstadoOrden.TERMINADO) {
            model.addAttribute("warn", "Esta orden ya está terminada.");
        }

        // Productos de chatarra para reciclaje GENERAL
        List<Producto> productosChatarra = ((List<Producto>) productoDao.findAll())
                .stream()
                .filter(p -> p.getNombre() != null && (
                        p.getNombre().toLowerCase().contains("chatarra") ||
                        p.getNombre().toLowerCase().contains("limadura") ||
                        p.getNombre().toLowerCase().contains("scrap")))
                .collect(Collectors.toList());

        model.addAttribute("orden", orden);
        model.addAttribute("productosChatarra", productosChatarra);
        model.addAttribute("titulo", "Cerrar Orden #" + id);
        return "produccion/formCerrarOrden";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CERRAR ORDEN — POST
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/cerrar")
    public String cerrarOrden(
            @PathVariable Long id,
            @RequestParam Double pesoProductoGr,
            @RequestParam Double pesoMermaGr,
            @RequestParam Double pesoReciclajeGr,
            @RequestParam String tipoReciclaje,
            @RequestParam(required = false) Long productoMermaId,
            Principal principal,
            RedirectAttributes flash) {

        try {
            joyeriaService.cerrarOrdenProduccion(
                    id, pesoProductoGr, pesoMermaGr, pesoReciclajeGr,
                    tipoReciclaje, productoMermaId,
                    principal != null ? principal.getName() : "Sistema");

            flash.addFlashAttribute("success",
                "Orden #" + id + " cerrada correctamente. " +
                "Producto: " + pesoProductoGr + "g | Merma: " + pesoMermaGr + "g | Reciclaje: " + pesoReciclajeGr + "g");
            return "redirect:/produccion";
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/produccion/" + id + "/cerrar";
        }
    }

    @Autowired private com.bolsadeideas.springboot.app.models.dao.LoteUsoRepository loteUsoRepo;

    // ══════════════════════════════════════════════════════════════════════════
    //  TRAZABILIDAD — historia de un lote
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/lote/{compraId}")
    public String historialLote(@PathVariable Long compraId, Model model) {
        CompraInventario lote = compraRepo.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + compraId));

        model.addAttribute("lote", lote);
        model.addAttribute("ordenes", joyeriaService.trazabilidadLote(compraId));
        model.addAttribute("usos", loteUsoRepo.findByCompraIdOrderByFechaUsoDesc(compraId));
        model.addAttribute("titulo", "Historial del Lote: " +
                (lote.getCodigoLote() != null ? lote.getCodigoLote() : "#" + compraId));
        return "produccion/historialLote";
    }
}
