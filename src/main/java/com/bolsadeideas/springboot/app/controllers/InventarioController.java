package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.dao.CompraInventarioRepository;
import com.bolsadeideas.springboot.app.models.dao.IProductoDao;
import com.bolsadeideas.springboot.app.models.dao.MovimientoStockRepository;
import com.bolsadeideas.springboot.app.models.dao.OrdenProduccionRepository;
import com.bolsadeideas.springboot.app.models.entity.CompraInventario;
import com.bolsadeideas.springboot.app.models.entity.MovimientoStock;
import com.bolsadeideas.springboot.app.models.entity.OrdenProduccion;
import com.bolsadeideas.springboot.app.models.entity.Producto;
import com.bolsadeideas.springboot.app.models.service.InventarioJoyeriaService;
import com.bolsadeideas.springboot.app.models.service.PedidoService;
import com.bolsadeideas.springboot.app.models.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inventario")
@Secured("ROLE_ADMIN")
public class InventarioController {

    @Autowired
    private CompraInventarioRepository compraRepo;

    @Autowired
    private MovimientoStockRepository movimientoRepo;

    @Autowired
    private IProductoDao productoDao;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private InventarioJoyeriaService joyeriaService;

    @Autowired
    private OrdenProduccionRepository ordenRepo;

    @Autowired
    private PedidoService pedidoService;

    // ─────────────────────────────────────────────
    //  DASHBOARD PRINCIPAL
    // ─────────────────────────────────────────────
    @GetMapping
    public String dashboard(Model model) {
        LocalDateTime ahora = LocalDateTime.now();

        // Períodos
        LocalDateTime inicioDia    = ahora.toLocalDate().atStartOfDay();
        LocalDateTime inicioSemana = ahora.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime inicioMes    = ahora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAnio   = ahora.toLocalDate().withDayOfYear(1).atStartOfDay();

        // KPIs — Compras
        model.addAttribute("comprasHoy",   compraRepo.totalGastadoEnPeriodo(inicioDia, ahora));
        model.addAttribute("comprasSemana",compraRepo.totalGastadoEnPeriodo(inicioSemana, ahora));
        model.addAttribute("comprasMes",   compraRepo.totalGastadoEnPeriodo(inicioMes, ahora));
        model.addAttribute("comprasAnio",  compraRepo.totalGastadoEnPeriodo(inicioAnio, ahora));

        // KPIs — Consumo/Salidas
        model.addAttribute("consumoHoy",   movimientoRepo.totalSalidasEnPeriodo(inicioDia, ahora));
        model.addAttribute("consumoSemana",movimientoRepo.totalSalidasEnPeriodo(inicioSemana, ahora));
        model.addAttribute("consumoMes",   movimientoRepo.totalSalidasEnPeriodo(inicioMes, ahora));
        model.addAttribute("consumoAnio",  movimientoRepo.totalSalidasEnPeriodo(inicioAnio, ahora));

        // Productos con stock bajo (< 5 unidades)
        List<Producto> stockBajo = ((List<Producto>) productoDao.findByCantidadLessThan(5.0)).stream()
                .limit(10).collect(Collectors.toList());
        model.addAttribute("stockBajo", stockBajo);

        // Últimas compras
        model.addAttribute("ultimasCompras", compraRepo.findTop10ByOrderByFechaCompraDesc());

        // Últimos movimientos
        model.addAttribute("ultimosMovimientos", movimientoRepo.findTop20ByOrderByFechaDesc());

        // Top materiales más consumidos este mes
        List<Object[]> topConsumo = movimientoRepo.topProductosConsumidos(inicioMes, ahora);
        model.addAttribute("topConsumo", topConsumo);

        // Total productos en inventario
        model.addAttribute("totalProductos", productoDao.count());

        // Datos para gráfico semanal de consumo (últimos 7 días)
        List<String> labels7d = new ArrayList<>();
        List<Double> data7d = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = LocalDate.now().minusDays(i);
            labels7d.add(dia.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es")));
            Double consumo = movimientoRepo.totalSalidasEnPeriodo(
                    dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
            data7d.add(consumo != null ? consumo : 0.0);
        }
        model.addAttribute("labels7d", labels7d);
        model.addAttribute("data7d", data7d);

        // Listas para formularios rápidos
        model.addAttribute("productos", productoDao.findAll());
        model.addAttribute("proveedores", proveedorService.findAll());

        // Lotes activos (con peso_actual_gr > 0) — para sección "Saldo por Lote"
        List<CompraInventario> lotesActivos = compraRepo.findTop50ByOrderByFechaCompraDesc()
                .stream()
                .filter(c -> c.getCodigoLote() != null
                          && c.getPesoActualGr() != null
                          && c.getPesoActualGr() > 0)
                .collect(Collectors.toList());
        model.addAttribute("lotesActivos", lotesActivos);

        // Órdenes de producción en proceso
        model.addAttribute("ordenesEnProceso",
                ordenRepo.findByEstadoOrderByFechaInicioDesc(OrdenProduccion.EstadoOrden.EN_PROCESO));

        // Pedidos para asignar consumo
        model.addAttribute("pedidos", pedidoService.findAllPedidos());

        model.addAttribute("titulo", "Centro de Inventario");
        return "inventario/dashboard";
    }

    // ─────────────────────────────────────────────
    //  LISTADO DE COMPRAS
    // ─────────────────────────────────────────────
    @GetMapping("/compras")
    public String listarCompras(Model model) {
        model.addAttribute("compras", compraRepo.findTop50ByOrderByFechaCompraDesc());
        model.addAttribute("titulo", "Historial de Compras");
        return "inventario/compras";
    }

    // ─────────────────────────────────────────────
    //  NUEVA COMPRA — formulario GET
    // ─────────────────────────────────────────────
    @GetMapping("/compras/nueva")
    public String nuevaCompraForm(Model model) {
        model.addAttribute("compra", new CompraInventario());
        model.addAttribute("productos", productoDao.findAll());
        model.addAttribute("proveedores", proveedorService.findAll());
        model.addAttribute("titulo", "Registrar Compra");
        return "inventario/formCompra";
    }

    // ─────────────────────────────────────────────
    //  GUARDAR COMPRA — POST
    // ─────────────────────────────────────────────
    @PostMapping("/compras/guardar")
    public String guardarCompra(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam Double cantidad,
            @RequestParam Double precioUnitario,
            @RequestParam(required = false) String numAlbaran,
            @RequestParam(required = false) String notas,
            // Campos de lote (joyería) — todos opcionales
            @RequestParam(required = false) Double pesoGramos,
            @RequestParam(required = false) String tipoMaterial,
            @RequestParam(required = false) String pureza,
            Principal principal,
            RedirectAttributes flash) {

        Producto producto = productoDao.findById(productoId).orElse(null);
        if (producto == null) {
            flash.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/inventario/compras/nueva";
        }

        // Determinar tipo y pureza (del producto si no se especifican en el form)
        String tipo   = (tipoMaterial != null && !tipoMaterial.isBlank()) ? tipoMaterial : producto.getTipoMaterial();
        String ley    = (pureza != null && !pureza.isBlank()) ? pureza : producto.getPureza();
        Double pesado = (pesoGramos != null && pesoGramos > 0) ? pesoGramos : cantidad;

        // Guardar compra
        CompraInventario compra = new CompraInventario();
        compra.setProducto(producto);
        if (proveedorId != null && proveedorId > 0) {
            compra.setProveedor(proveedorService.findOne(proveedorId));
        }
        compra.setCantidad(cantidad);
        compra.setPrecioUnitario(precioUnitario);
        compra.setPrecioTotal(cantidad * precioUnitario);
        compra.setFechaCompra(LocalDateTime.now());
        compra.setNumAlbaran(numAlbaran);
        compra.setNotas(notas);
        compra.setUsuarioRegistro(principal != null ? principal.getName() : "Sistema");

        // ── Campos de lote (joyería) ──────────────────────────────────────────
        if (tipo != null && !tipo.isBlank()) {
            compra.setTipoMaterial(tipo);
            compra.setPureza(ley);
            compra.setPesoInicialGr(pesado);
            compra.setPesoActualGr(pesado);  // empieza igual
            // Generar código de lote automáticamente
            String codigoLote = joyeriaService.generarCodigoLote(tipo, ley);
            compra.setCodigoLote(codigoLote);
        }

        compraRepo.save(compra);

        // Actualizar stock del producto
        producto.setCantidad(producto.getCantidad() + cantidad);
        productoDao.save(producto);

        // Registrar movimiento de ENTRADA
        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(producto);
        mov.setTipo(MovimientoStock.TipoMovimiento.ENTRADA);
        mov.setCantidad(cantidad);
        mov.setFecha(LocalDateTime.now());
        mov.setCompraId(compra.getId());
        mov.setMotivo("Compra a proveedor - Albarán: " + (numAlbaran != null ? numAlbaran : "—")
            + (compra.getCodigoLote() != null ? " | Lote: " + compra.getCodigoLote() : ""));
        mov.setStockResultante(producto.getCantidad());
        mov.setUsuarioRegistro(principal != null ? principal.getName() : "Sistema");
        movimientoRepo.save(mov);

        String msg = "Compra registrada. Stock: " + producto.getCantidad() + " unidades.";
        if (compra.getCodigoLote() != null) msg += " Lote: " + compra.getCodigoLote();
        flash.addFlashAttribute("success", msg);
        return "redirect:/inventario/compras";
    }

    // ─────────────────────────────────────────────
    //  REGISTRAR SALIDA / CONSUMO — POST
    // ─────────────────────────────────────────────
    @PostMapping("/salida")
    public String registrarSalida(
            @RequestParam Long productoId,
            @RequestParam Double cantidad,
            @RequestParam(required = false) String referenciaPedido,
            @RequestParam(required = false) String motivo,
            Principal principal,
            RedirectAttributes flash) {

        Producto producto = productoDao.findById(productoId).orElse(null);
        if (producto == null) {
            flash.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/inventario";
        }
        if (producto.getCantidad() < cantidad) {
            flash.addFlashAttribute("error", "Stock insuficiente. Disponible: " + producto.getCantidad());
            return "redirect:/inventario";
        }

        producto.setCantidad(producto.getCantidad() - cantidad);
        productoDao.save(producto);

        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(producto);
        mov.setTipo(MovimientoStock.TipoMovimiento.SALIDA);
        mov.setCantidad(cantidad);
        mov.setFecha(LocalDateTime.now());
        mov.setReferenciaPedido(referenciaPedido);
        mov.setMotivo(motivo != null ? motivo : "Consumo en taller");
        mov.setStockResultante(producto.getCantidad());
        mov.setUsuarioRegistro(principal != null ? principal.getName() : "Sistema");
        movimientoRepo.save(mov);

        flash.addFlashAttribute("success", "Salida registrada. Stock restante: " + producto.getCantidad());
        return "redirect:/inventario";
    }

    // ─────────────────────────────────────────────
    //  API JSON — KPI en tiempo real (polling)
    // ─────────────────────────────────────────────
    @GetMapping("/api/kpi")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kpiJson() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioDia    = ahora.toLocalDate().atStartOfDay();
        LocalDateTime inicioSemana = ahora.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime inicioMes    = ahora.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioAnio   = ahora.toLocalDate().withDayOfYear(1).atStartOfDay();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("comprasHoy",    compraRepo.totalGastadoEnPeriodo(inicioDia, ahora));
        kpi.put("comprasSemana", compraRepo.totalGastadoEnPeriodo(inicioSemana, ahora));
        kpi.put("comprasMes",    compraRepo.totalGastadoEnPeriodo(inicioMes, ahora));
        kpi.put("comprasAnio",   compraRepo.totalGastadoEnPeriodo(inicioAnio, ahora));
        kpi.put("consumoHoy",    movimientoRepo.totalSalidasEnPeriodo(inicioDia, ahora));
        kpi.put("consumoSemana", movimientoRepo.totalSalidasEnPeriodo(inicioSemana, ahora));
        kpi.put("consumoMes",    movimientoRepo.totalSalidasEnPeriodo(inicioMes, ahora));
        kpi.put("consumoAnio",   movimientoRepo.totalSalidasEnPeriodo(inicioAnio, ahora));
        kpi.put("stockBajoCount", ((List<?>) productoDao.findByCantidadLessThan(5.0)).size());
        kpi.put("timestamp", ahora.toString());
        return ResponseEntity.ok(kpi);
    }

    // ─────────────────────────────────────────────
    //  API JSON — Datos gráfico consumo 7 días
    // ─────────────────────────────────────────────
    @GetMapping("/api/grafico7d")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> grafico7d() {
        List<String> labels = new ArrayList<>();
        List<Double> compras = new ArrayList<>();
        List<Double> consumo = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate dia = LocalDate.now().minusDays(i);
            labels.add(dia.toString());
            Double c = compraRepo.totalGastadoEnPeriodo(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
            Double s = movimientoRepo.totalSalidasEnPeriodo(dia.atStartOfDay(), dia.atTime(LocalTime.MAX));
            compras.add(c != null ? c : 0.0);
            consumo.add(s != null ? s : 0.0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("compras", compras);
        result.put("consumo", consumo);
        return ResponseEntity.ok(result);
    }
}
