package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Cita;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.service.IDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Controlador para el Dashboard de Gestión.
 */
@Controller
@RequestMapping("/admin/dashboard-gestion")
@Secured({"ROLE_ADMIN", "ROLE_USER"})
public class AdminDashboardController {

    @Autowired private IDashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {

        // ── Rango de fechas: mes actual ────────────────────────────────────────
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date inicioMes = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        Date finMes = cal.getTime();

        LocalDateTime inicioMesLdt = inicioMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime finMesLdt    = finMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // ── KPIs básicos ──────────────────────────────────────────────────────
        Double facturacionTotal = dashboardService.getFacturacionTotal();
        long pedidosActivos = dashboardService.getPedidosActivosCount();
        long clientesNuevosMes = dashboardService.getClientesNuevosMesCount(inicioMes, finMes);
        Double comprasMes = dashboardService.getComprasMesTotal(inicioMesLdt, finMesLdt);
        
        // Beneficio del mes = Facturación de este mes - Compras de este mes
        Double facturacionMes = dashboardService.getFacturacionMesTotal(inicioMes, finMes);
        Double beneficioMes = facturacionMes - comprasMes;

        // Listas de actividad
        List<OrdenProduccion> ordenesRecientes = dashboardService.getOrdenesRecientes();
        List<Pedido> pedidosRecientes = dashboardService.getPedidosRecientes();
        List<Cliente> clientesRecientes = dashboardService.getClientesRecientes();
        List<Cita> citasProximas = dashboardService.getCitasProximas();

        // ── Gráficos ──────────────────────────────────────────────────────────
        String[] labelsGrafico = new String[7];
        Double[] dataCompras = new Double[7];
        Double[] dataVentas = new Double[7];
        Double[] dataBeneficio = new Double[7];
        
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = LocalDate.now().minusDays(i);
            LocalDateTime desdeLdt = dia.atStartOfDay();
            LocalDateTime hastaLdt = dia.atTime(23, 59, 59);
            
            Date desdeDate = Date.from(desdeLdt.atZone(ZoneId.systemDefault()).toInstant());
            Date hastaDate = Date.from(hastaLdt.atZone(ZoneId.systemDefault()).toInstant());

            Double c = dashboardService.getIngresosDia(desdeLdt, hastaLdt);
            Double v = dashboardService.getFacturacionDia(desdeDate, hastaDate);
            
            dataCompras[6 - i] = c;
            dataVentas[6 - i] = v;
            dataBeneficio[6 - i] = v - c;
            
            labelsGrafico[6 - i] = dia.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.SHORT, new java.util.Locale("es", "ES"));
        }

        Map<String, Long> statusDistrib = dashboardService.getPedidosPorStatus();

        String usuario = principal != null ? principal.getName() : "Usuario";

        model.addAttribute("titulo", "Dashboard de Gestión");
        model.addAttribute("facturacionTotal", facturacionTotal);
        model.addAttribute("pedidosActivos", pedidosActivos);
        model.addAttribute("clientesNuevosMes", clientesNuevosMes);
        model.addAttribute("comprasMes", comprasMes);
        model.addAttribute("beneficioMes", beneficioMes);
        
        model.addAttribute("ordenesRecientes", ordenesRecientes);
        model.addAttribute("pedidosRecientes", pedidosRecientes);
        model.addAttribute("clientesRecientes", clientesRecientes);
        model.addAttribute("citasProximas", citasProximas);
        
        model.addAttribute("labelsGrafico", labelsGrafico);
        model.addAttribute("dataCompras", dataCompras);
        model.addAttribute("dataVentas", dataVentas);
        model.addAttribute("dataBeneficio", dataBeneficio);
        model.addAttribute("statusDistrib", statusDistrib);
        
        model.addAttribute("nombreUsuario", usuario);

        return "admin/dashboard_gestion";
    }
}
