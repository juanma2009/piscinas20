package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.apigoogledrice.GoogleDriveService;
import com.bolsadeideas.springboot.app.apisms.AppSms;
import com.bolsadeideas.springboot.app.models.entity.*;
import com.bolsadeideas.springboot.app.models.service.*;
import com.bolsadeideas.springboot.app.models.service.redis.RedisQueueProducer;
import com.bolsadeideas.springboot.app.models.service.redis.RedisTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.bolsadeideas.springboot.app.util.paginator.PageRender;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pedidos")
@SessionAttributes({"pedido", "fotosTemporales"})
@Log4j2
public class PedidoController {

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private ProveedorServiceImpl proveedorService;

    @Autowired
    private PedidoServiceImpl pedidoService;

    @Autowired
    private IUploadFileService uploadFileService;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private ArchivoAdjuntoService archivoAdjuntoService;

    @Autowired
    private RedisTestService redisTestService;

    @Autowired
    private RedisQueueProducer redisQueueProducer;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @ModelAttribute("fotosTemporales")
    public List<String> inicializarFotosTemporales() {
        return new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        // Inicialización necesaria al arrancar el controlador
    }

    static final String TITULO = "titulo";
    static final String ERROR = "error";
    static final String REDIRECTLISTAR = "redirect:/pedidolistar";
    static final String PEDIDOFORM = "/pedido/pedidoform";
    static final String CREARPEDIDO = "Crear Pedido";

    private void agregarDatosOpcionesAModelo(Map<String, Object> model) {
        List<String> estados = Arrays.asList("Finalizado", "Pendiente");
        model.put("estados", estados);

        List<String> servicios = Arrays.asList("Pedido", "Compostela");
        model.put("servicios", servicios);

        List<String> metales = Arrays.asList("Oro Amarillo", "Oro Blanco", "Oro Rosa", "Plata", "Platino", "Otro");
        model.put("metales", metales);

        List<String> piezas = Arrays.asList("Anillo", "Colgante", "Pulsera", "Pendientes", "Aro", "Broche", "Otros");
        model.put("piezas", piezas);

        Map<String, List<String>> tiposPorPieza = new HashMap<>();
        tiposPorPieza.put("Anillo", Arrays.asList("Anillo", "Alianzas", "1/2 Alianzas", "Solitarios", "Sello"));
        tiposPorPieza.put("Colgante", Arrays.asList("Con Piedra", "Sin Piedra"));
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));
        model.put("tiposPorPieza", tiposPorPieza);
    }


    @GetMapping("/ver/fotos/{fileId}")
    public ResponseEntity<ByteArrayResource> verFoto(@PathVariable String fileId) {
        try {
            // Obtener la URL segura del archivo desde Cloudinary
            String imageUrl = cloudinaryService.downloadImage(fileId).toString(); // Método que obtiene la URL segura de la imagen
            log.info(imageUrl);
            // Descargar el archivo desde Cloudinary
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Leer el flujo de entrada
            InputStream in = connection.getInputStream();
            byte[] data = IOUtils.toByteArray(in);
            ByteArrayResource resource = new ByteArrayResource(data);

            // Especificar el tipo de contenido como imagen (si se conoce el tipo)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);  // Asegúrate de que el tipo sea el correcto

            return ResponseEntity.ok().headers(headers).contentLength(data.length).body(resource);
        } catch (IOException e) {
            // En caso de error, retornar 404
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar la imagen desde Cloudinary", e);
        }
    }


    @RequestMapping(value = "/listarPedidos", method = RequestMethod.GET)
    public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        Pageable pageRequest = PageRequest.of(page, 5);
        Page<Pedido> pedido = pedidoService.findAllPedidos(pageRequest);

        log.info("Listado de {} pedidos obtenidos", pedido.getTotalElements());
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);
        model.addAttribute(TITULO, "Listado de Pedidos");

        agregarDatosOpcionesAModelo(model.asMap());

        return "pedido/pedidolistar";
    }


    @RequestMapping(value = {"/listarFotosPedidos"}, method = RequestMethod.GET)
    public String listarFotos(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedido = pedidoService.findAll(pageRequest);

        log.info("Listado de {} pedidos con fotos obtenidos", pedido.getTotalElements());
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute(TITULO, "Galería de Pedidos");
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);

        agregarDatosOpcionesAModelo(model.asMap());

        return "pedido/pedidofotolistar";
    }


    @GetMapping("/ver/{id}")
    public String ver(@PathVariable(value = "id") Long id, Model model, RedirectAttributes flash) {
        Pedido pedido = clienteService.findPedidoById(id);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe en la base de datos!");
            return "redirect:/listar";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("titulo", "Detalles del Pedido");
        return "pedido/pedidover";
    }


    /**
     * Cargar las imágenes asociadas a un pedido
     *
     * @param id
     * @return
     */


    @GetMapping("/cargarImagenes/{id}")
    @ResponseBody
    public List<String> cargarImagenes(@PathVariable(value = "id") Long id) throws Exception {
        List<String> urls = new ArrayList<>();

        // Obtener los archivos adjuntos asociados al pedido
        List<ArchivoAdjunto> archivosAdjuntos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(id);

        for (ArchivoAdjunto archivo : archivosAdjuntos) {
            if (archivo.getUrlCloudinary() != null) {  // Cambia esto a la propiedad de Cloudinary
                // Construir la URL pública para cada archivo desde Cloudinary
                String url = cloudinaryService.getImageUrl(archivo.getUrlCloudinary());
                urls.add(url);
            } else {
                log.warn("El archivo con nombre " + archivo.getNombre() + " no tiene un Cloudinary Public ID asociado.");
            }
        }

        return urls;
    }


    /**
     * Crear los Pedidos para los clientes
     *
     * @param clienteId
     * @param model
     * @param flash
     * @return
     */
    @GetMapping("/form/{clienteId}")
    public String crear(@PathVariable(value = "clienteId") Long clienteId, Map<String, Object> model, RedirectAttributes flash) {
        Cliente cliente = clienteService.findOne(clienteId);
        if (cliente == null) {
            flash.addFlashAttribute(ERROR, "El cliente no existe en la base de datos");
            return REDIRECTLISTAR;
        }

        log.info("Abriendo formulario de pedido para cliente: {}", cliente.getNombre());
        Pedido numeroPedido = pedidoService.obtenerUltimoNumeroPedido();
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);

        model.put("numeroPedido", numeroPedido.getNpedido() + 1);
        model.put("pedido", pedido);
        model.put("proveedores", proveedorService.findAll());
        model.put("empleado", List.of("Anselmo"));
        model.put(TITULO, CREARPEDIDO);

        agregarDatosOpcionesAModelo(model);

        return "pedido/pedidoform";
    }


    private void actualizarPedidoExistente(Pedido pedidoExistente, String observacion, String estado, String tipoPedido, String grupo, String pieza, Double peso, String horas, Double cobrado, Date fechaEntrega, Date fechaFinalizado, String empleado, String ref, RedirectAttributes flash) {
        log.info("📝 Iniciando actualización de pedido: {}", pedidoExistente.getNpedido());
        
        pedidoExistente.setObservacion(observacion);
        pedidoExistente.setEstado(estado);
        pedidoExistente.setTipoPedido(tipoPedido);
        pedidoExistente.setGrupo(grupo);
        pedidoExistente.setSubgrupo(pieza);
        pedidoExistente.setPeso(peso);
        pedidoExistente.setHoras(horas);
        pedidoExistente.setCobrado(cobrado);
        pedidoExistente.setFechaFinalizado(fechaFinalizado);
        pedidoExistente.setFechaEntrega(fechaEntrega);
        pedidoExistente.setRef(ref);
        pedidoExistente.setEmpleado(empleado);
        pedidoExistente.setPieza(pieza);
        
        log.info("📋 Datos actualizados - Estado: {}, Peso: {}, Cobrado: {}", estado, peso, cobrado);

        try {
            if ("Fin".equalsIgnoreCase(pedidoExistente.getEstado()) && !pedidoExistente.getEnviadoSms()) {
                enviarSms(pedidoExistente);
            }
            pedidoService.save(pedidoExistente);
            flash.addFlashAttribute("info", "Pedido actualizado con éxito");
            log.info("Pedido {} actualizado correctamente", pedidoExistente.getNpedido());
        } catch (Exception e) {
            log.error("Error al actualizar el pedido {}: {}", pedidoExistente.getNpedido(), e.getMessage(), e);
            flash.addFlashAttribute("error", "Error al actualizar el pedido: " + e.getMessage());
        }
    }

    private void enviarSms(Pedido pedidoExistente) {
        String destinatario = pedidoExistente.getCliente().getTelefono();
        String mensaje = "Su pedido: " + pedidoExistente.getNpedido() + " ha sido finalizado. Gracias por su confianza, puede pasar a recoger cuando pueda!";

        AppSms sms = new AppSms();
        ResponseEntity<String> response = sms.sendMessage(destinatario, mensaje);

        if (response.getStatusCode() == HttpStatus.OK) {
            pedidoExistente.setEnviadoSms(true);
            pedidoExistente.setEstadoEnvioSms("Mensaje: " + response.getStatusCode());
            pedidoExistente.setFechaEnvioSms(new Date());
            log.info("Mensaje enviado con éxito: " + response.getBody());
        } else {
            pedidoExistente.setEstadoEnvioSms("Error en el envío: " + response.getStatusCode());
            log.error("Error al enviar el mensaje: " + response.getStatusCode());
        }
    }

    private void guardarNuevoPedido(Pedido pedido, RedirectAttributes flash) {
        try {
            log.info("💾 Guardando nuevo pedido - npedido: {}, cliente: {}", pedido.getNpedido(), pedido.getCliente().getNombre());
            pedidoService.save(pedido);
            log.info("✅ Pedido guardado exitosamente - npedido: {}", pedido.getNpedido());
            flash.addFlashAttribute("info", "Pedido guardado con éxito");
        } catch (Exception e) {
            log.error("❌ ERROR al guardar el pedido: {}", e.getMessage(), e);
            flash.addFlashAttribute("error", "Error al guardar el pedido: " + e.getMessage());
            throw new RuntimeException("Error al guardar el pedido: " + e.getMessage(), e);
        }
    }

    @PostMapping("/form")
    public ResponseEntity<?> guardar(
            @ModelAttribute @Valid Pedido pedido,
            BindingResult result, Model model,
            @RequestParam(name = "observacion", required = false) String observacion,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "tipoPedido", required = false) String tipoPedido,
            @RequestParam(name = "grupo", required = false) String grupo,
            @RequestParam(name = "pieza", required = false) String pieza,
            @RequestParam(name = "peso", required = false) String peso,
            @RequestParam(name = "horas", required = false) String horas,
            @RequestParam(name = "cobrado", required = false) String cobrado,
            @RequestParam(name = "fechaFinalizado", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaFinalizado,
            @RequestParam(name = "fechaEntrega", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaEntrega,
            @RequestParam(name = "empleado", required = false) String empleado,
            @RequestParam(name = "ref", required = false) String ref,
            RedirectAttributes flash, SessionStatus status,
            @RequestParam(name = "fileNamesJSON", required = false) String fileNamesJSON,
            @RequestParam(name = "files", required = false) MultipartFile[] files

    ) {
        try {
            log.info("🔵 INICIO - Guardando pedido (SOLO DATOS DEL FORMULARIO, SIN ARCHIVOS)");
            log.info("   Cliente: {} {}", pedido.getCliente() != null ? pedido.getCliente().getNombre() : "NULL", pedido.getCliente() != null ? pedido.getCliente().getId() : "");
            log.info("   Parámetros recibidos:");
            log.info("     - observacion: {}", observacion != null ? observacion.substring(0, Math.min(50, observacion.length())) : "NULL");
            log.info("     - estado: {}", estado);
            log.info("     - tipoPedido: {}", tipoPedido);
            log.info("     - grupo: {}", grupo);
            log.info("     - pieza: {}", pieza);
            log.info("     - peso: {}", peso);
            log.info("     - horas: {}", horas);
            log.info("     - cobrado: {}", cobrado);
            log.info("     - empleado: {}", empleado);
            log.info("     - ref: {}", ref);
            log.info("ℹ️ Archivos recibidos (solo referencia): {}", files != null ? files.length : 0);
            if (files != null && files.length > 0) {
                log.info("   Nota: Los {} archivo(s) se subirán en una petición SEPARADA después de crear/actualizar el pedido", files.length);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error al loguear información inicial", e);
        }

        try {
            if (result.hasErrors()) {
                log.warn("⚠️ Errores de validación en el formulario del pedido");
                java.util.List<String> errores = result.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(java.util.stream.Collectors.toList());
                log.error("   Errores encontrados: {}", errores);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Existen errores en el formulario",
                    "details", errores
                ));
            }

            Long npedido = pedido.getNpedido();
            log.info("📊 Parseando parámetros...");
            
            Double pesoDouble = parsePeso(peso);
            Double cobradoDouble = parseCobrado(cobrado);
            String horasSaneadas = parseHoras(horas);
            sanitizeClienteNombre(pedido);
            
            log.info("📊 Parámetros parseados:");
            log.info("   - peso: {} -> {}", peso, pesoDouble);
            log.info("   - cobrado: {} -> {}", cobrado, cobradoDouble);
            log.info("   - horas: {} -> {}", horas, horasSaneadas);

            boolean esActualizacion = npedido != null && npedido > 0;
            log.info("📊 ¿Es actualización? {} (npedido: {})", esActualizacion, npedido);

            if (esActualizacion) {
                Pedido pedidoExistente = pedidoService.findOne(npedido);
                if (pedidoExistente != null) {
                    log.info("🔄 Actualizando pedido existente: {}", npedido);
                    actualizarPedidoExistente(pedidoExistente, observacion, estado, tipoPedido, grupo, pieza,
                            pesoDouble, horasSaneadas, cobradoDouble, fechaEntrega,
                            fechaFinalizado, empleado, ref, flash);

                    status.setComplete();
                    log.info("✅ Pedido actualizado correctamente");
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("info", "Pedido actualizado con éxito");
                    response.put("npedido", pedidoExistente.getNpedido());
                    response.put("clienteId", pedidoExistente.getCliente().getId());
                    response.put("tieneArchivos", files != null && files.length > 0);
                    
                    if (files != null && files.length > 0) {
                        log.info("📁 {} archivo(s) pendientes de subir asincronamente para pedido {}", files.length, npedido);
                        response.put("archivosASubir", files.length);
                    }
                    
                    return ResponseEntity.ok(response);
                } else {
                    log.warn("⚠️ Pedido con npedido={} no encontrado. Se creará uno nuevo.", npedido);
                }
            }

            log.info("🆕 Creando nuevo pedido para cliente: {}", pedido.getCliente().getNombre());
            guardarNuevoPedido(pedido, flash);

            status.setComplete();
            log.info("✅ Pedido {} creado correctamente (SIN ARCHIVOS)", pedido.getNpedido());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("info", "Pedido creado con éxito");
            response.put("npedido", pedido.getNpedido());
            response.put("clienteId", pedido.getCliente().getId());
            response.put("isNew", true);
            response.put("tieneArchivos", files != null && files.length > 0);
            
            if (files != null && files.length > 0) {
                log.info("📁 {} archivo(s) pendientes de subir asincronamente para pedido {}", files.length, pedido.getNpedido());
                response.put("archivosASubir", files.length);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ ERROR CRÍTICO en guardar pedido: {}", e.getMessage());
            log.error("   Tipo de excepción: {}", e.getClass().getName());
            log.error("   Causa: {}", e.getCause() != null ? e.getCause().getMessage() : "Sin causa");
            log.error("   Stack trace:", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al guardar el pedido");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            errorResponse.put("fullType", e.getClass().getName());
            errorResponse.put("timestamp", new java.util.Date());
            
            if (e.getCause() != null) {
                errorResponse.put("cause", e.getCause().getMessage());
            }
            
            StackTraceElement[] stackTraceElements = e.getStackTrace();
            if (stackTraceElements.length > 0) {
                errorResponse.put("failedAt", stackTraceElements[0].getClassName() + "." + stackTraceElements[0].getMethodName() + " línea " + stackTraceElements[0].getLineNumber());
            }
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

// -------------------------------------------------------------------------
// 4. Métodos auxiliares para sanitización de datos
// -------------------------------------------------------------------------

    private Double parsePeso(String peso) {

        if (peso == null || peso.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // Reemplazar coma por punto
            String normalizado = peso.trim().replace(",", ".");
            return Double.parseDouble(normalizado);

        } catch (NumberFormatException e) {
            // ⚠️ Manda un mensaje de error al front
            throw new IllegalArgumentException("El campo PESO solo admite valores numéricos.");
        }
    }


    // Método para convertir el valor de 'cobrado' de String a Double
    private Double parseCobrado(String cobrado) {
        if (cobrado != null && !cobrado.trim().isEmpty()) {
            return Double.valueOf(cobrado.trim().replace(",", "."));
        }
        return 0.0;  // Si no hay cobrado, devolvemos 0.0
    }

    // Método para convertir el valor de 'horas' de String a formato adecuado
    private String parseHoras(String horas) {
        if (horas != null && !horas.trim().isEmpty()) {
            return horas.trim().replace(",", ":");
        }
        return "0";  // Si no hay horas, devolvemos "0"
    }

    // Método para concatenar el nombre y apellido del cliente
    private void sanitizeClienteNombre(Pedido pedido) {
        if (pedido.getCliente().getNombre() != null && pedido.getCliente().getApellido() != null) {
            String nombreCompleto = pedido.getCliente().getNombre() + " " + pedido.getCliente().getApellido();
            pedido.getCliente().setNombre(nombreCompleto);
        }
    }


// ---------------------------------------------------------------------------------
// MÉTODO AUXILIAR para Subir Archivos (se centraliza la lógica de /guardarFotos)
// ---------------------------------------------------------------------------------
private void subirYGuardarArchivos(MultipartFile[] fotos, Long npedido, Long clienteId, RedirectAttributes flash) {
    log.info("📸 INICIO subirYGuardarArchivos - npedido: {}, clienteId: {}, totalArchivos: {}", npedido, clienteId, fotos != null ? fotos.length : 0);
    
    if (fotos == null || fotos.length == 0) {
        log.warn("⚠️ No hay archivos para subir");
        return;
    }

    int archivosProcesados = 0;
    StringBuilder errores = new StringBuilder();

    for (MultipartFile foto : fotos) {
        String nombreOriginal = foto.getOriginalFilename();
        long tamaño = foto.getSize();
        String contentType = foto.getContentType();
        
        log.info("🔍 Validando archivo:");
        log.info("   Nombre: {}", nombreOriginal);
        log.info("   Tamaño: {} bytes", tamaño);
        log.info("   MIME Type: {}", contentType);
        
        if (foto.isEmpty()) {
            log.warn("❌ Archivo vacío: {}", nombreOriginal);
            errores.append("• Archivo vacío (0 bytes): ").append(nombreOriginal).append(" - ¿De Google Drive? Descárgalo primero.\n");
            continue;
        }

        if (!validarTipoMime(contentType, nombreOriginal)) {
            String error = "Archivo no es una imagen válida (MIME: " + contentType + "): " + nombreOriginal;
            log.warn("❌ {}", error);
            errores.append("• ").append(error).append("\n");
            continue;
        }
        
        log.info("✅ Archivo validado correctamente: {}", nombreOriginal);

        try {
            byte[] imageBytes = foto.getBytes();

            if (imageBytes.length == 0) {
                log.warn("Archivo sin contenido: {}", nombreOriginal);
                errores.append("• Archivo vacío: ").append(nombreOriginal).append("\n");
                continue;
            }

            String fileName = "pedido_" + npedido + "_" + System.currentTimeMillis();

            log.info("Subiendo archivo {} ({}  KB) a Cloudinary...", nombreOriginal, imageBytes.length / 1024);
            String imageUrl = cloudinaryService.uploadImage(imageBytes, npedido, fileName);

            log.info("Archivo subido con éxito: {} -> {}", nombreOriginal, imageUrl);

            String mensaje = npedido + ";" + nombreOriginal + ";" + fileName;
            redisTemplate.opsForValue().set("archivo_" + npedido + "_" + System.currentTimeMillis(), mensaje);
            redisQueueProducer.sendMessage(mensaje);

            archivosProcesados++;
            log.info("Mensaje enviado a Redis para procesamiento: {}", mensaje);

        } catch (IOException e) {
            String error = "Error al leer archivo " + nombreOriginal + ": " + e.getMessage();
            log.error(error, e);
            errores.append("• ").append(error).append("\n");
        } catch (Exception e) {
            String error = "Error al subir " + nombreOriginal + " a Cloudinary: " + e.getMessage();
            log.error(error, e);
            errores.append("• ").append(error).append("\n");
        }
    }

    if (archivosProcesados > 0) {
        log.info("✅ {} archivo(s) subido(s) correctamente", archivosProcesados);
        flash.addFlashAttribute("info", "✓ " + archivosProcesados + " archivo(s) subido(s) con éxito.");
    }

    if (errores.length() > 0) {
        log.warn("⚠️ Errores al procesar archivos:\n{}", errores.toString());
        flash.addFlashAttribute("warning", "Algunos archivos no se pudieron procesar:\n" + errores.toString());
    }

    if (archivosProcesados == 0 && errores.length() > 0) {
        log.error("❌ Ningún archivo válido procesado. Detalles: {}", errores.toString());
    } else if (archivosProcesados == 0 && errores.length() == 0) {
        log.warn("⚠️ No se enviaron archivos");
    }
    
    log.info("🎬 FIN subirYGuardarArchivos - Procesados: {}/{}, Errores: {}", archivosProcesados, fotos.length, errores.length() > 0 ? "Sí" : "No");
}

private boolean validarTipoMime(String contentType, String fileName) {
    if (contentType == null) {
        contentType = "";
    }

    // Tipos MIME válidos
    String[] tiposValidos = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"};
    boolean esValidoPorMime = false;

    for (String tipo : tiposValidos) {
        if (contentType.contains(tipo)) {
            esValidoPorMime = true;
            break;
        }
    }

    // Validar también por extensión del archivo (importante para Google Drive)
    String extension = fileName != null ? fileName.toLowerCase() : "";
    boolean esValidoPorExtension = extension.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$");

    // Si Google Drive no envía MIME type, validar por extensión
    if (contentType.isEmpty() || contentType.equals("application/octet-stream")) {
        log.info("📌 Archivo sin MIME type o tipo genérico. Validando por extensión: {}", extension);
        return esValidoPorExtension;
    }

    return esValidoPorMime || esValidoPorExtension;
}



    /**
     * Eliminar los pedido que interesen
     *
     * @param id
     * @param flash
     * @return
     */
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash) {

        Pedido pedido = clienteService.findPedidoById(id);
        if (pedido != null) {
            // Eliminar todos los archivos adjuntos asociados al pedido
            //  for (ArchivoAdjunto archivoAdjunto : pedido.getArchivos()) {
            // Aquí podrías llamar a algún servicio o método para eliminar el archivo adjunto de la base de datos
            //archivoAdjuntoService.eliminarArchivoAdjunto(archivoAdjunto);            }

            // Eliminar el pedido
            clienteService.deletePedido(id);

            flash.addFlashAttribute("success", "Pedido eliminado con éxito!");
            return "redirect:/ver/" + pedido.getCliente().getId();
        }
        flash.addFlashAttribute(ERROR, "El pedido no existe en la base de datos, no se pudo eliminar!");
        return REDIRECTLISTAR;
    }


    /**
     * Filtro para buscar los ALBARANES
     *
     * @param page
     * @param id
     * @param pageable
     * @param model
     * @return
     */
    //todo añadir los nuevos campos metal,pieza,tipo
    @PostMapping("/buscar")
    public String buscar(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "cliente", defaultValue = "") int id, @RequestParam(name = "estado", defaultValue = "") String estado, @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido, @RequestParam(name = "grupo", defaultValue = "") String grupo, @RequestParam(name = "pieza", defaultValue = "") String pieza, @RequestParam(name = "tipo", defaultValue = "") String tipo, @RequestParam(name = "ref", defaultValue = "") String ref, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta, Pageable pageable, Model model) {


        // Si no se proporciona fecha, usa null
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        // Paginación de las búsquedas totales
        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedido = pedidoService.buscarPedidos(id, tipoPedido, estado, grupo, pieza, tipo, ref, fechaDesdeDate, fechaHastaDate, pageRequest);
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Agregar la lista de imágenes de los pedidos
        // Agregar la lista de imágenes de los pedidos
        Map<Long, String> imagenesPedidos = new HashMap<>();
        for (Pedido p : pedido.getContent()) {
            // Obtener los archivos adjuntos del pedido
            List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());

            if (!archivos.isEmpty()) {
                // El fileName ya contiene el publicId, por lo que lo usamos directamente
                String publicId = archivos.get(0).getUrlCloudinary();  // Asumimos que getUrlCloudinary() ya devuelve el publicId

                try {
                    // Recuperar la URL usando el publicId directamente
                    String imageUrl = cloudinaryService.getImageUrl(publicId);
                    imagenesPedidos.put(p.getNpedido(), imageUrl);
                } catch (Exception e) {
                    // Si ocurre un error, asignar la imagen por defecto
                    imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
                    log.error("Error al obtener la imagen para el pedido {}: {}", p.getNpedido(), e.getMessage());
                }
            } else {
                // Si no hay archivos adjuntos, asignar imagen por defecto
                imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
            }
        }



        model.addAttribute("imagenesPedidos", imagenesPedidos);

        // Datos estáticos (estados, grupos, subgrupos, etc.)
        List<String> estados = Arrays.asList("Finalizado", "Pendiente");
        model.addAttribute("estados", estados);
        //todo añadir al metodo editar los nuevos campos metal,pieza,tipo

        // 1. Listado de Servicio
        List<String> servicios = Arrays.asList("Pedido", "Compostela");
        model.addAttribute("servicios", servicios);
        // 2. Listado de Metal
        List<String> metales = Arrays.asList("Oro Amarillo", "Oro Blanco", "Oro Rosa", "Plata", "Platino", "Otro");
        model.addAttribute("metales", metales);
        // 3. Listado de Pieza (padre de Tipos)
        List<String> piezas = Arrays.asList("Anillo", "Colgante", "Pulsera", "Pendientes", "Aro", "Broche", "Otros");
        model.addAttribute("piezas", piezas);
        // 4. Mapa de Tipos según Pieza
        Map<String, List<String>> tiposPorPieza = new HashMap<>();
        tiposPorPieza.put("Anillo", Arrays.asList("Anillo", "Alianzas", "1/2 Alianzas", "Solitarios", "Sello"));
        tiposPorPieza.put("Colgante", Arrays.asList("Con Piedra", "Sin Piedra"));
        // Pulsera: tendrá solo “Pulsera” pero escondemos el select en el front
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        // Broche y Otros: solo una opción y ocultamos el listbox en el front
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));
        model.addAttribute("tiposPorPieza", tiposPorPieza);
        // Obtener los clientes para el filtro
        model.addAttribute("clientes", clienteService.findAll());

        // Filtros seleccionados (si no se pasa nada, se deja vacío o con valor predeterminado)
        model.addAttribute("clienteSeleccionado", id);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("grupoSeleccionado", grupo);
        model.addAttribute("tipoPedidoSeleccionado", tipoPedido);
        model.addAttribute("subgrupoSeleccionado", pieza);
        //todo poner tipo
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("fechaDesdeSeleccionada", fechaDesde != null ? fechaDesde : "");
        model.addAttribute("fechaHastaSeleccionada", fechaHasta != null ? fechaHasta : "");

        // Otros valores
        model.addAttribute("TITULO", "Lista de Pedidos Encontradas ");
        model.addAttribute("textoR", "Resultados Encontrados: ");
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);
        model.addAttribute("countProveedor", proveedorService.count());

        return "pedido/pedidolistar";
    }


    @PostMapping("/buscarFoto")
    public String buscarFoto(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(required = false) Integer id, @RequestParam(required = false) String servicios, @RequestParam(required = false) String estado, @RequestParam(required = false) String grupo, @RequestParam(required = false) String pieza, @RequestParam(required = false) String tipo, @RequestParam(required = false) String ref, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta, Pageable pageable, Model model) throws JsonProcessingException {

        log.info("Buscar foto cliente id{}", id);
        // Conversión de fechas
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        // Búsqueda paginada
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedido = pedidoService.buscarPedidos(id, servicios, estado, grupo, pieza, tipo, ref, fechaDesdeDate, fechaHastaDate, pageRequest);
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Mapear imágenes de pedidos
        Map<Long, String> imagenesPedidos = new HashMap<>();
        for (Pedido p : pedido.getContent()) {
            // Obtener los archivos adjuntos del pedido
            List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());

            if (!archivos.isEmpty()) {
                try {
                    // Usar el publicId almacenado en el archivo adjunto (por ejemplo, archivos.get(0).getPublicId())
                    String publicId = archivos.get(0).getUrlCloudinary();  // Asumiendo que el publicId está almacenado en el objeto `ArchivoAdjunto`

                    // Intentar obtener la URL de la imagen desde Redis
                    String imageUrl = cloudinaryService.obtenerImagen(publicId);  // Recupera la URL de Cloudinary o Redis

                    // Guardar la URL en el mapa para el pedido
                    imagenesPedidos.put(p.getNpedido(), imageUrl);

                } catch (Exception e) {
                    // Si ocurre un error, asignar la imagen por defecto
                    imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
                }
            } else {
                // Si no hay archivos adjuntos, asignar la imagen por defecto
                imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
            }
        }

        // Añadir resultados y filtros al modelo
        model.addAttribute("busquedaRealizada", true);
        model.addAttribute("imagenesPedidos", imagenesPedidos);
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);
        model.addAttribute("countProveedor", proveedorService.count());

        // Filtros activos
        model.addAttribute("clienteSeleccionado", id);
        model.addAttribute("servicioSeleccionado", servicios);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("metalSeleccionado", grupo);
        model.addAttribute("piezaSeleccionada", pieza);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("refSeleccionado", ref);
        model.addAttribute("fechaDesdeSeleccionada", fechaDesde != null ? fechaDesde : "");
        model.addAttribute("fechaHastaSeleccionada", fechaHasta != null ? fechaHasta : "");

        // Cargar datos para selectores
        List<String> estados = Arrays.asList("Finalizado", "Pendiente");
        model.addAttribute("estados", estados);
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("servicios", Arrays.asList("Pedido", "Compostela"));
        model.addAttribute("metales", Arrays.asList("Oro Amarillo", "Oro Blanco", "Oro Rosa", "Plata", "Platino", "Otro"));
        model.addAttribute("piezas", Arrays.asList("Anillo", "Colgante", "Pulsera", "Pendientes", "Aro", "Broche", "Otros"));

        Map<String, List<String>> tiposPorPieza = new HashMap<>();
        tiposPorPieza.put("Anillo", Arrays.asList("Anillo", "Alianzas", "1/2 Alianzas", "Solitarios", "Sello"));
        tiposPorPieza.put("Colgante", Arrays.asList("Con Piedra", "Sin Piedra"));
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));

        log.info("Mapa de tiposPorPieza: {}", tiposPorPieza); // Usa la interpolación de cadenas

        // Serializa el objeto a JSON usando ObjectMapper
        model.addAttribute("tiposPorPieza", tiposPorPieza);

        ObjectMapper objectMapper = new ObjectMapper();
        String tiposPorPiezaJson = objectMapper.writeValueAsString(tiposPorPieza);
        model.addAttribute("tiposPorPiezaJson", tiposPorPiezaJson);
        log.info("Mapa de tiposPorPieza: {}", tiposPorPiezaJson); // Usa la interpolación de cadenas
        // Texto del encabezado
        model.addAttribute("TITULO", "Lista de Pedidos Encontrados");
        model.addAttribute("textoR", "Resultados Encontrados:");

        return "pedido/pedidofotolistar";
    }


    //muetra ls fotos de lso pedidos

    @GetMapping("/eliminarFoto/{fileId}")
    public String eliminarFoto(@PathVariable("fileId") String fileId, @RequestParam("npedido") String pedidoId, RedirectAttributes flash) {
        log.info("Llegan del front: pedidoId=" + pedidoId + ", fileId=" + fileId);

        // Buscar el archivo en la base de datos relacionado con este pedido
        Optional<ArchivoAdjunto> archivo = archivoAdjuntoService.findArchivosAdjuntosByPedidoIdOne(fileId, Long.valueOf(pedidoId));

        log.info("Archivo encontrado: " + archivo);

        if (archivo.isPresent()) {
            try {
                // Eliminar de Cloudinary
                String deleteResult = cloudinaryService.deleteImage(archivo.get().getUrlCloudinary()); // Aquí debes cambiar el método a obtener el publicId de Cloudinary

                if ("ok".equals(deleteResult)) {
                    // Eliminar la referencia en la base de datos
                    archivoAdjuntoService.eliminarArchivoAdjunto(archivo.orElse(null));
                    flash.addFlashAttribute("info", "Foto eliminada con éxito de Cloudinary y del pedido!");
                } else {
                    flash.addFlashAttribute("error", "No se encontró la imagen en Cloudinary.");
                }

            } catch (Exception e) {
                flash.addFlashAttribute("error", "Error al eliminar la foto: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            flash.addFlashAttribute("error", "No se encontró la foto asociada.");
        }

        return "redirect:/pedidos/form/" + pedidoId;
    }


    //todo añadir los nuevos campos metal,pieza,tipo
    @RequestMapping(value = "/formEditar/{id}")
    public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

        Pedido pedido = null;

        if (id > 0) {
            pedido = pedidoService.findPedidoById(id);
            if (pedido == null) {
                flash.addFlashAttribute("error", "El ID del Pedido no existe en la BBDD!");
                return "redirect:pedido/listar";
            }
        } else {
            flash.addFlashAttribute("error", "El ID del Pedido no puede ser cero!");
            return "redirect:pedido/listar";
        }

        List<String> estados = Arrays.asList("Finalizado", "Pendiente");
        model.put("estados", estados);
        //todo añadir al metodo editar los nuevos campos metal,pieza,tipo

        // 1. Listado de Servicio
        List<String> servicios = Arrays.asList("Pedido", "Compostela");
        model.put("servicios", servicios);
        // 2. Listado de Metal
        List<String> metales = Arrays.asList("Oro Amarillo", "Oro Blanco", "Oro Rosa", "Plata", "Platino", "Otro");
        model.put("metales", metales);
        // 3. Listado de Pieza (padre de Tipos)
        List<String> piezas = Arrays.asList("Anillo", "Colgante", "Pulsera", "Pendientes", "Aro", "Broche", "Otros");
        model.put("piezas", piezas);
        // 4. Mapa de Tipos según Pieza
        Map<String, List<String>> tiposPorPieza = new HashMap<>();
        tiposPorPieza.put("Anillo", Arrays.asList("Anillo", "Alianzas", "1/2 Alianzas", "Solitarios", "Sello"));
        tiposPorPieza.put("Colgante", Arrays.asList("Con Piedra", "Sin Piedra"));
        // Pulsera: tendrá solo “Pulsera” pero escondemos el select en el front
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        // Broche y Otros: solo una opción y ocultamos el listbox en el front
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));
        model.put("tiposPorPieza", tiposPorPieza);

        // Empleados
        List<String> empleado = List.of("Anselmo");
        model.put("empleado", empleado);

        model.put("pedido", pedido);
        model.put("titulo", "Editar Pedido");
        return "pedido/pedidoform";
    }

    @PostMapping("/report")
    public ResponseEntity<?> generateReport(HttpServletResponse response, @RequestParam(name = "cliente") String cliente, @RequestParam(name = "estado") String estado) {
        log.info("cliente: " + cliente);
        log.info("estado: " + estado);
        try {
            // Generar el informe
            JasperPrint jasperPrint = pedidoService.generateJasperPrint(cliente, estado);

            // Convertir el informe a un arreglo de bytes en formato PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            // Configurar la respuesta HTTP para la descarga del PDF
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=pedido_report.pdf");

            // Enviar el arreglo de bytes en la respuesta
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            //redirigir a lista de pedidos

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Redirige al formulario de edición de un pedido específico para un cliente dado.
     *
     * @param clienteId El ID del cliente asociado al pedido.
     * @param pedidoId  El ID del pedido que se desea editar.
     * @return Una redirección a la página de edición del pedido o a la lista si no se encuentra el cliente o el pedido.
     */
    @GetMapping("/pedidos/form/{clienteId}/{pedidoId}")
    public String editarPedido(@PathVariable Long clienteId, @PathVariable Long pedidoId) {
        Cliente cliente = clienteService.findOne(clienteId);
        Pedido pedido = pedidoService.findOne(pedidoId);

        if (cliente == null || pedido == null) {
            return "redirect:/listar"; // o página de error
        }

        return "redirect:/pedidos/formEditar/{}" + pedidoId;
    }

    @PostMapping("/subir-archivos/{npedido}")
    public ResponseEntity<?> subirArchivos(
            @PathVariable Long npedido,
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            RedirectAttributes flash,
            HttpServletRequest request
    ) {
        try {
            log.info("🔵 ========== INICIO subirArchivos ==========");
            log.info("📍 Endpoint: /pedidos/subir-archivos/{}", npedido);
            log.info("📊 Content-Type header: {}", request.getContentType());
            log.info("📊 Method: {}", request.getMethod());
            log.info("📊 Files array: {}", files != null ? "NOT NULL" : "NULL");
            log.info("📸 Total archivos recibidos: {}", files != null ? files.length : 0);
            
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    MultipartFile f = files[i];
                    log.info("   Archivo[{}]: name={}, size={} bytes, contentType={}, empty={}", 
                        i, f.getOriginalFilename(), f.getSize(), f.getContentType(), f.isEmpty());
                }
            } else {
                log.warn("⚠️ NO HAY ARCHIVOS - files es null o vacío");
            }
            
            Pedido pedido = pedidoService.findOne(npedido);
            if (pedido == null) {
                log.warn("❌ Pedido {} no encontrado", npedido);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Pedido no encontrado",
                    "npedido", npedido
                ));
            }

            if (files == null || files.length == 0) {
                log.info("✅ No hay archivos para subir");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "info", "No hay archivos para subir",
                    "archivosSubidos", 0
                ));
            }

            int archivosProcesados = 0;
            StringBuilder errores = new StringBuilder();

            for (MultipartFile foto : files) {
                String nombreOriginal = foto.getOriginalFilename();
                long tamaño = foto.getSize();
                String contentType = foto.getContentType();
                
                log.info("🔍 Validando archivo: {} ({} bytes, MIME: {})", nombreOriginal, tamaño, contentType);
                
                if (foto.isEmpty()) {
                    log.warn("❌ Archivo vacío: {}", nombreOriginal);
                    errores.append("• Archivo vacío (0 bytes): ").append(nombreOriginal).append("\n");
                    continue;
                }

                if (!validarTipoMime(contentType, nombreOriginal)) {
                    String error = "Archivo no es una imagen válida (MIME: " + contentType + "): " + nombreOriginal;
                    log.warn("❌ {}", error);
                    errores.append("• ").append(error).append("\n");
                    continue;
                }
                
                log.info("✅ Archivo validado: {}", nombreOriginal);

                try {
                    byte[] imageBytes = foto.getBytes();

                    if (imageBytes.length == 0) {
                        log.warn("Archivo sin contenido: {}", nombreOriginal);
                        errores.append("• Archivo vacío: ").append(nombreOriginal).append("\n");
                        continue;
                    }

                    String fileName = "pedido_" + npedido + "_" + System.currentTimeMillis();

                    log.info("Subiendo archivo {} ({} KB) a Cloudinary...", nombreOriginal, imageBytes.length / 1024);
                    String imageUrl = cloudinaryService.uploadImage(imageBytes, npedido, fileName);

                    log.info("Archivo subido con éxito: {} -> {}", nombreOriginal, imageUrl);

                    String mensaje = npedido + ";" + nombreOriginal + ";" + fileName;
                    redisTemplate.opsForValue().set("archivo_" + npedido + "_" + System.currentTimeMillis(), mensaje);
                    redisQueueProducer.sendMessage(mensaje);

                    archivosProcesados++;
                    log.info("Mensaje enviado a Redis: {}", mensaje);

                } catch (IOException e) {
                    String error = "Error al leer archivo " + nombreOriginal + ": " + e.getMessage();
                    log.error(error, e);
                    errores.append("• ").append(error).append("\n");
                } catch (Exception e) {
                    String error = "Error al subir " + nombreOriginal + " a Cloudinary: " + e.getMessage();
                    log.error(error, e);
                    errores.append("• ").append(error).append("\n");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", archivosProcesados > 0);
            response.put("archivosSubidos", archivosProcesados);
            response.put("npedido", npedido);

            if (archivosProcesados > 0) {
                log.info("✅ {} archivo(s) subido(s)", archivosProcesados);
                response.put("info", "✓ " + archivosProcesados + " archivo(s) subido(s) con éxito.");
            }

            if (errores.length() > 0) {
                log.warn("⚠️ Errores al procesar archivos:\n{}", errores.toString());
                response.put("warnings", errores.toString());
            }

            log.info("🎬 FIN subirArchivos - Procesados: {}/{}", archivosProcesados, files.length);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ ERROR en subirArchivos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error al subir archivos",
                "message", e.getMessage()
            ));
        }
    }


}
