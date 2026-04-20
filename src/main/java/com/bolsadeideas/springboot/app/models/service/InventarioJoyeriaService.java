package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.dao.*;
import com.bolsadeideas.springboot.app.models.entity.*;
import com.bolsadeideas.springboot.app.models.entity.MovimientoStock.TipoMovimiento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Servicio núcleo del sistema de inventario avanzado para taller de joyería.
 *
 * Gestiona:
 *  - Generación de códigos de lote
 *  - Consumo parcial de lotes (con validación de peso)
 *  - Cierre de órdenes de producción (balance de masa + 3 tipos de reciclaje)
 *  - Trazabilidad completa lote → órdenes
 */
@Service
@Transactional
public class InventarioJoyeriaService {

    @Autowired private CompraInventarioRepository compraRepo;
    @Autowired private MovimientoStockRepository   movimientoRepo;
    @Autowired private IProductoDao                productoDao;
    @Autowired private LoteUsoRepository           loteUsoRepo;
    @Autowired private OrdenProduccionRepository   ordenRepo;
    @Autowired private OrdenProduccionLoteRepository ordenLoteRepo;
    @Autowired private PedidoDao                   pedidoDao;

    // ══════════════════════════════════════════════════════════════════════════
    //  1. CÓDIGO DE LOTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Genera un código de lote único para una compra de material.
     * Formato: TIPO-PUREZA-YYYYMMDD-SEQ
     * Ejemplo: ORO-18K-20240420-001
     *
     * @param tipoMaterial "ORO", "PLATA", "PLATINO", "OTRO" (nullable → "MAT")
     * @param pureza       "18k", "925", etc. (nullable → "XX")
     */
    public String generarCodigoLote(String tipoMaterial, String pureza) {
        String fecha   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tipo    = tipoMaterial != null
                ? tipoMaterial.substring(0, Math.min(3, tipoMaterial.length())).toUpperCase()
                : "MAT";
        String pureStr = pureza != null
                ? pureza.toUpperCase().replace(" ", "").replace(".", "")
                : "XX";
        long seq = compraRepo.count() + 1;
        return String.format("%s-%s-%s-%03d", tipo, pureStr, fecha, seq);
    }

    /**
     * Genera código de lote para reciclaje.
     * Ejemplo: RECIC-ORO-18K-20240420-001
     */
    public String generarCodigoLoteReciclaje(String codigoLoteOrigen) {
        return "RECIC-" + codigoLoteOrigen;
    }

    /**
     * Genera código de lote para merma general acumulada.
     * Ejemplo: MERMA-GEN-20240420
     */
    public String generarCodigoMermaGeneral() {
        return "MERMA-GEN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. CONSUMO DE LOTE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Consume parte de un lote (CompraInventario).
     * Resta el peso del lote y del stock del producto.
     * Registra el uso en LoteUso y un MovimientoStock(CONSUMO_LOTE).
     *
     * @param compraId          ID del lote (CompraInventario)
     * @param pesoGr            Gramos a consumir
     * @param pedidoId          Pedido asociado (nullable)
     * @param ordenProduccionId Orden de producción asociada (nullable)
     * @param motivo            Descripción del uso
     * @param usuario           Usuario que registra
     * @throws IllegalStateException si el lote no tiene suficiente peso
     */
    public LoteUso consumirDeLote(Long compraId, Double pesoGr,
                                   Long pedidoId, Long ordenProduccionId,
                                   String motivo, String usuario) {

        CompraInventario lote = compraRepo.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + compraId));

        // Validar peso disponible
        double disponible = lote.getPesoActualGr() != null ? lote.getPesoActualGr() : lote.getCantidad();
        if (disponible < pesoGr) {
            throw new IllegalStateException(
                "Peso insuficiente en lote " + (lote.getCodigoLote() != null ? lote.getCodigoLote() : compraId)
                + ". Disponible: " + disponible + "g | Solicitado: " + pesoGr + "g");
        }

        // 1. Restar del lote
        if (lote.getPesoActualGr() != null) {
            lote.setPesoActualGr(lote.getPesoActualGr() - pesoGr);
        }

        // 2. Restar del stock del producto
        Producto producto = lote.getProducto();
        producto.setCantidad(Math.max(0, producto.getCantidad() - pesoGr));
        productoDao.save(producto);
        compraRepo.save(lote);

        // 3. Registrar en LoteUso (trazabilidad)
        LoteUso uso = new LoteUso();
        uso.setCompra(lote);
        uso.setPesoUsadoGr(pesoGr);
        if (pedidoId != null) {
            uso.setPedido(pedidoDao.findById(pedidoId).orElse(null));
        }
        uso.setOrdenProduccionId(ordenProduccionId);
        uso.setMotivo(motivo);
        uso.setUsuarioRegistro(usuario);
        loteUsoRepo.save(uso);

        // 4. Movimiento de auditoría
        registrarMovimiento(producto, TipoMovimiento.CONSUMO_LOTE, pesoGr,
            "Lote: " + (lote.getCodigoLote() != null ? lote.getCodigoLote() : compraId)
            + " | " + (motivo != null ? motivo : ""),
            usuario, producto.getCantidad());

        return uso;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. CIERRE DE ORDEN DE PRODUCCIÓN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cierra una orden de producción registrando el resultado del trabajo.
     *
     * Valida balance de masa: pesoEntrada = pesoProducto + pesoMerma + pesoReciclaje
     *
     * Según tipoReciclaje:
     *   "LOTE"    → crea nuevo lote hijo con la pureza del lote principal
     *   "GENERAL" → acumula en producto genérico de chatarra (productoMermaId)
     *   "NINGUNO" → solo registra la merma informativa, no crea stock
     *
     * @param ordenId          ID de la OrdenProduccion
     * @param pesoProductoGr   Gramos en la pieza final
     * @param pesoMermaGr      Gramos perdidos irrecuperablemente
     * @param pesoReciclajeGr  Gramos recuperables
     * @param tipoReciclaje    "LOTE" | "GENERAL" | "NINGUNO"
     * @param productoMermaId  Requerido solo si tipoReciclaje = "GENERAL"
     * @param usuario          Usuario que cierra la orden
     */
    public OrdenProduccion cerrarOrdenProduccion(Long ordenId,
                                                  Double pesoProductoGr,
                                                  Double pesoMermaGr,
                                                  Double pesoReciclajeGr,
                                                  String tipoReciclaje,
                                                  Long productoMermaId,
                                                  String usuario) {

        OrdenProduccion orden = ordenRepo.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + ordenId));

        if (orden.getEstado() == OrdenProduccion.EstadoOrden.TERMINADO) {
            throw new IllegalStateException("La orden " + ordenId + " ya está terminada");
        }

        // ── Validar balance de masa ──────────────────────────────────────────
        double totalSalida = pesoProductoGr + pesoMermaGr + pesoReciclajeGr;
        if (Math.abs(totalSalida - orden.getPesoEntradaGr()) > 0.01) {
            throw new IllegalArgumentException(
                "Balance de masa incorrecto. " +
                "Entrada: " + orden.getPesoEntradaGr() + "g | " +
                "Salida: " + totalSalida + "g " +
                "(producto=" + pesoProductoGr + " + merma=" + pesoMermaGr
                + " + reciclaje=" + pesoReciclajeGr + ")");
        }

        // ── Actualizar orden ─────────────────────────────────────────────────
        orden.setPesoProductoGr(pesoProductoGr);
        orden.setPesoMermaGr(pesoMermaGr);
        orden.setPesoReciclajeGr(pesoReciclajeGr);
        orden.setTipoReciclaje(tipoReciclaje);
        orden.setEstado(OrdenProduccion.EstadoOrden.TERMINADO);
        orden.setFechaFin(LocalDateTime.now());
        orden.setUsuarioRegistro(usuario);

        // ── ACTUALIZACIÓN AUTOMÁTICA DEL PEDIDO ──────────────────────────
        if (orden.getPedido() != null) {
            Pedido p = orden.getPedido();
            // Solo marcar como TERMINADO si no quedan más órdenes EN_PROCESO para este pedido
            long ordenesPendientes = ordenRepo.countByPedidoNpedidoAndEstado(p.getNpedido(), OrdenProduccion.EstadoOrden.EN_PROCESO);
            
            if (ordenesPendientes == 0) {
                p.setEstado("TERMINADO");
                p.setFechaFinalizado(new java.util.Date());
                pedidoDao.save(p);
            }
        }

        // ── Registrar merma irrecuperable ────────────────────────────────────
        if (pesoMermaGr > 0) {
            // El stock ya bajó al consumir el lote; este movimiento es solo auditoría
            Producto prodPrincipal = obtenerProductoPrincipal(orden);
            if (prodPrincipal != null) {
                registrarMovimiento(prodPrincipal, TipoMovimiento.MERMA, pesoMermaGr,
                    "Merma irrecuperable — Orden " + ordenId, usuario, prodPrincipal.getCantidad());
            }
        }

        // ── Lógica de reciclaje ──────────────────────────────────────────────
        if (pesoReciclajeGr > 0 && tipoReciclaje != null) {
            switch (tipoReciclaje.toUpperCase()) {

                case "LOTE": {
                    // Misma pureza y material que el lote principal → nuevo lote hijo
                    CompraInventario loteBase = obtenerLotePrincipal(orden);
                    if (loteBase == null) {
                        throw new IllegalStateException(
                            "No se encontró lote principal para reciclaje tipo LOTE en orden " + ordenId);
                    }

                    CompraInventario loteReciclado = new CompraInventario();
                    loteReciclado.setProducto(loteBase.getProducto());
                    loteReciclado.setProveedor(null); // no tiene proveedor, es reciclaje
                    loteReciclado.setCodigoLote(generarCodigoLoteReciclaje(
                            loteBase.getCodigoLote() != null ? loteBase.getCodigoLote() : "LOTE-" + loteBase.getId()));
                    loteReciclado.setTipoMaterial(loteBase.getTipoMaterial());
                    loteReciclado.setPureza(loteBase.getPureza()); // misma pureza asumida
                    loteReciclado.setPesoInicialGr(pesoReciclajeGr);
                    loteReciclado.setPesoActualGr(pesoReciclajeGr);
                    loteReciclado.setCantidad(pesoReciclajeGr);
                    loteReciclado.setPrecioUnitario(0.0);
                    loteReciclado.setPrecioTotal(0.0);
                    loteReciclado.setNotas("Reciclaje LOTE de orden " + ordenId
                            + " (lote origen: " + loteBase.getCodigoLote() + ")");
                    loteReciclado.setUsuarioRegistro(usuario);
                    compraRepo.save(loteReciclado);

                    // Subir stock del mismo Producto
                    Producto prod = loteBase.getProducto();
                    prod.setCantidad(prod.getCantidad() + pesoReciclajeGr);
                    productoDao.save(prod);

                    orden.setLoteReciclajeId(loteReciclado.getId());

                    registrarMovimiento(prod, TipoMovimiento.RECICLAJE_IN, pesoReciclajeGr,
                        "Reciclaje LOTE de orden " + ordenId, usuario, prod.getCantidad());
                    break;
                }

                case "GENERAL": {
                    // Merma acumulada sin trazabilidad de lote → producto genérico chatarra
                    if (productoMermaId == null) {
                        throw new IllegalArgumentException(
                            "Para tipoReciclaje=GENERAL se requiere productoMermaId");
                    }
                    Producto prodChatarra = productoDao.findById(productoMermaId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                "Producto de chatarra no encontrado: " + productoMermaId));

                    CompraInventario entradaChatarra = new CompraInventario();
                    entradaChatarra.setProducto(prodChatarra);
                    entradaChatarra.setCodigoLote(generarCodigoMermaGeneral());
                    entradaChatarra.setPesoInicialGr(pesoReciclajeGr);
                    entradaChatarra.setPesoActualGr(pesoReciclajeGr);
                    entradaChatarra.setCantidad(pesoReciclajeGr);
                    entradaChatarra.setPrecioUnitario(0.0);
                    entradaChatarra.setPrecioTotal(0.0);
                    entradaChatarra.setNotas(
                        "Merma GENERAL de orden " + ordenId + " — sin lote origen trazable");
                    entradaChatarra.setUsuarioRegistro(usuario);
                    compraRepo.save(entradaChatarra);

                    // Acumular en stock del producto chatarra
                    prodChatarra.setCantidad(prodChatarra.getCantidad() + pesoReciclajeGr);
                    productoDao.save(prodChatarra);

                    orden.setProductoMermaId(productoMermaId);

                    registrarMovimiento(prodChatarra, TipoMovimiento.RECICLAJE_IN, pesoReciclajeGr,
                        "Merma GENERAL de orden " + ordenId, usuario, prodChatarra.getCantidad());
                    break;
                }

                case "NINGUNO":
                default:
                    // Nada recuperable. La merma ya se registró arriba como auditoría.
                    break;
            }
        }

        return ordenRepo.save(orden);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  4. ABRIR ORDEN DE PRODUCCIÓN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Crea una nueva orden de producción y consume los lotes indicados.
     *
     * @param pedidoId    ID del pedido del cliente (nullable)
     * @param descripcion Descripción del trabajo
     * @param lotesYPesos Map<compraId, pesoGr> — lotes a consumir con su peso
     * @param usuario     Usuario
     */
    public OrdenProduccion abrirOrdenProduccion(Long pedidoId, String descripcion,
                                                 Map<Long, Double> lotesYPesos,
                                                 String usuario) {
        if (lotesYPesos == null || lotesYPesos.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un lote y su peso");
        }

        double totalEntrada = lotesYPesos.values().stream().mapToDouble(Double::doubleValue).sum();

        OrdenProduccion orden = new OrdenProduccion();
        if (pedidoId != null) {
            Pedido p = pedidoDao.findById(pedidoId).orElse(null);
            orden.setPedido(p);
        }
        orden.setDescripcion(descripcion);
        orden.setPesoEntradaGr(totalEntrada);
        orden.setUsuarioRegistro(usuario);
        ordenRepo.save(orden);

        // Consumir cada lote y registrar OrdenProduccionLote
        for (Map.Entry<Long, Double> entry : lotesYPesos.entrySet()) {
            Long compraId = entry.getKey();
            Double pesoGr = entry.getValue();

            // Consumir del lote (valida peso disponible y actualiza stock)
            consumirDeLote(compraId, pesoGr, pedidoId, orden.getId(),
                "Orden de producción: " + descripcion, usuario);

            // Registrar relación orden ↔ lote
            CompraInventario lote = compraRepo.findById(compraId).orElseThrow();
            OrdenProduccionLote opl = new OrdenProduccionLote();
            opl.setOrdenProduccion(orden);
            opl.setCompra(lote);
            opl.setPesoUsadoGr(pesoGr);
            ordenLoteRepo.save(opl);
        }

        return orden;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  5. TRAZABILIDAD
    // ══════════════════════════════════════════════════════════════════════════

    /** ¿En qué órdenes se usó un lote? */
    @Transactional(readOnly = true)
    public List<OrdenProduccion> trazabilidadLote(Long compraId) {
        return ordenRepo.findOrdenesPorLote(compraId);
    }

    /** ¿Qué lotes se usaron en un pedido? */
    @Transactional(readOnly = true)
    public List<LoteUso> lotesUsadosEnPedido(Long pedidoId) {
        return loteUsoRepo.findByPedidoId(pedidoId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS INTERNOS
    // ══════════════════════════════════════════════════════════════════════════

    private void registrarMovimiento(Producto producto, TipoMovimiento tipo,
                                      Double cantidad, String motivo,
                                      String usuario, Double stockResultante) {
        MovimientoStock mov = new MovimientoStock();
        mov.setProducto(producto);
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setMotivo(motivo);
        mov.setUsuarioRegistro(usuario);
        mov.setStockResultante(stockResultante);
        movimientoRepo.save(mov);
    }

    /** Obtiene el primer lote (CompraInventario) usado en la orden */
    private CompraInventario obtenerLotePrincipal(OrdenProduccion orden) {
        List<OrdenProduccionLote> lotes = ordenLoteRepo.findByOrdenProduccionId(orden.getId());
        return lotes.isEmpty() ? null : lotes.get(0).getCompra();
    }

    /** Obtiene el Producto del primer lote usado en la orden */
    private Producto obtenerProductoPrincipal(OrdenProduccion orden) {
        CompraInventario lote = obtenerLotePrincipal(orden);
        return lote != null ? lote.getProducto() : null;
    }
}
