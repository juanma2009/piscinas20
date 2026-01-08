package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.apigoogledrice.GoogleDriveService;
import com.bolsadeideas.springboot.app.apisms.AppSms;
import com.bolsadeideas.springboot.app.models.entity.*;
import com.bolsadeideas.springboot.app.models.service.*;
import com.bolsadeideas.springboot.app.models.service.redis.RedisQueueProducer;
import com.bolsadeideas.springboot.app.models.service.redis.RedisTestService;
import com.bolsadeideas.springboot.app.util.paginator.PageRender;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
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

    @Autowired
    private GoogleDriveApiService googleDriveApiService;

    @Autowired
    private ArchivoSubidaService archivoSubidaService;

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
    public String listar(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(required = false) String activo,
                         Authentication authentication, Model model) {
        Pageable pageRequest = PageRequest.of(page, 5);
        Page<Pedido> pedido = pedidoService.findAllPedidos(pageRequest);
        Boolean activoBool = null;

        // Solo los admins pueden ver pedidos dados de baja
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if ("false".equals(activo) && !isAdmin) {
            // Si un usuario no admin intenta ver los de baja → ignoramos el filtro
            activoBool = true;  // Fuerza solo activos
        } else if (activo != null && !activo.isEmpty()) {
            activoBool = Boolean.valueOf(activo);
        }
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


    private void actualizarPedidoExistente(Pedido pedidoExistente, String observacion, String estado, String tipoPedido, String grupo, String pieza,String tipo, Double peso, String horas, Double cobrado, Date fechaEntrega, Date fechaFinalizado, String empleado, String ref, RedirectAttributes flash) {
        log.info("📝 Iniciando actualización de pedido: {}", pedidoExistente.getNpedido());
        
        pedidoExistente.setObservacion(observacion);
        pedidoExistente.setEstado(estado);
        pedidoExistente.setTipoPedido(tipoPedido);
        pedidoExistente.setGrupo(grupo);
        pedidoExistente.setSubgrupo(pieza);
        pedidoExistente.setTipo(tipo);
        pedidoExistente.setPeso(peso);
        pedidoExistente.setHoras(horas);
        pedidoExistente.setCobrado(cobrado);
        pedidoExistente.setFechaFinalizado(fechaFinalizado);
        pedidoExistente.setFechaEntrega(fechaEntrega);
        pedidoExistente.setRef(ref);
        pedidoExistente.setEmpleado(empleado);
        pedidoExistente.setPieza(pieza);
        
        log.info("📋 Datos actualizados - Estado: {}, Peso: {}, Cobrado: {},tipo:{}", estado, peso, cobrado,tipo);

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

    private Pedido guardarNuevoPedido(Pedido pedido, RedirectAttributes flash) {
        try {
            log.info("💾 Guardando nuevo pedido para cliente: {}", pedido.getCliente().getNombre());

            // Guardamos y obtenemos la entidad gestionada con el ID generado
            Pedido pedidoGuardado = pedidoService.save(pedido);

            log.info("✅ Pedido creado con éxito - npedido generado: {}", pedidoGuardado.getNpedido());

            flash.addFlashAttribute("info", "Pedido creado con éxito");

            return pedidoGuardado;  // ← DEVOLVEMOS LA ENTIDAD CON ID

        } catch (Exception e) {
            log.error("❌ ERROR al guardar nuevo pedido: {}", e.getMessage(), e);
            flash.addFlashAttribute("error", "Error al guardar el pedido: " + e.getMessage());
            throw new RuntimeException("Error al guardar el pedido", e);
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
            @RequestParam(name = "tipo", required = false) String tipo,
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
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            @RequestParam(name = "googleDriveFileIds", required = false) String[] googleDriveFileIds,
            @RequestParam(name = "googleDriveToken", required = false) String googleDriveToken,
            @RequestParam(name = "npedido", required = false) Long npedido,
            Principal principal  // ← Usuario autenticado
    ) {
        try {
            log.info("🔵 INICIO - Guardando pedido (SOLO DATOS DEL FORMULARIO, SIN ARCHIVOS)");
            log.info("   Cliente: {} {}", pedido.getCliente() != null ? pedido.getCliente().getNombre() : "NULL", pedido.getCliente() != null ? pedido.getCliente().getId() : "");

            log.info("ℹ️ Archivos locales: {}, Archivos Google Drive: {}",
                    files != null ? files.length : 0,
                    googleDriveFileIds != null ? googleDriveFileIds.length : 0);

            if (result.hasErrors()) {
                List<String> errores = result.getFieldErrors().stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .collect(Collectors.toList());
                log.error("   Errores encontrados: {}", errores);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Existen errores en el formulario",
                        "details", errores
                ));
            }

            // Parseo de parámetros (tu código existente)
            Double pesoDouble = parsePeso(peso);
            Double cobradoDouble = parseCobrado(cobrado);
            String horasSaneadas = parseHoras(horas);
            sanitizeClienteNombre(pedido);

            boolean esActualizacion = npedido != null && npedido > 0;

            if (esActualizacion) {
                Pedido pedidoExistente = pedidoService.findOne(npedido);
                if (pedidoExistente != null) {
                    actualizarPedidoExistente(pedidoExistente, observacion, estado, tipoPedido, grupo, pieza, tipo,
                            pesoDouble, horasSaneadas, cobradoDouble, fechaEntrega, fechaFinalizado, empleado, ref, flash);

                    status.setComplete();

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("info", "Pedido actualizado con éxito");
                    response.put("npedido", pedidoExistente.getNpedido());
                    response.put("clienteId", pedidoExistente.getCliente().getId());
                    response.put("tieneArchivos", files != null && files.length > 0 || googleDriveFileIds != null && googleDriveFileIds.length > 0);

                    return ResponseEntity.ok(response);
                }
            }

            // === CREACIÓN DE NUEVO PEDIDO ===
            log.info("🆕 Creando nuevo pedido para cliente: {}", pedido.getCliente().getNombre());

            Pedido pedidoGuardado = guardarNuevoPedido(pedido, flash);
            Long npedidoGenerado = pedidoGuardado.getNpedido();

            if (npedidoGenerado == null) {
                log.error("❌ CRÍTICO: npedido es null después de guardar!");
                throw new RuntimeException("Error interno: no se pudo generar el número de pedido");
            }

            status.setComplete();

            // === RESPUESTA INMEDIATA AL USUARIO ===
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("info", "Pedido creado con éxito. Los archivos se están procesando en segundo plano...");
            response.put("npedido", npedidoGenerado);
            response.put("clienteId", pedidoGuardado.getCliente().getId());
            response.put("isNew", true);

            // Detectamos si hay archivos (locales o Drive)
            boolean hayArchivos = (files != null && files.length > 0) ||
                    (googleDriveFileIds != null && googleDriveFileIds.length > 0);

            response.put("tieneArchivos", hayArchivos);

            if (hayArchivos) {
                // Obtenemos el userId del usuario autenticado
                String userId = (principal != null && principal.getName() != null)
                        ? principal.getName()
                        : "anonymous";

                // Serializamos los IDs de Google Drive
                String googleDriveIdsSerializados = googleDriveFileIds != null
                        ? String.join(",", googleDriveFileIds)
                        : "";

                // Formato de la tarea: npedido|userId|googleDriveIds|googleDriveToken
                String tarea = npedidoGenerado + "|" +
                        userId + "|" +
                        googleDriveIdsSerializados + "|" +
                        (googleDriveToken != null ? googleDriveToken : "");

                // Encolamos en Redis
                redisTemplate.opsForList().leftPush("cola:procesar-archivos", tarea);

                log.info("📤 Tarea encolada en Redis para pedido {} - Usuario: {} - Drive files: {}",
                        npedidoGenerado, userId, googleDriveFileIds != null ? googleDriveFileIds.length : 0);

                response.put("archivosASubir",
                        (files != null ? files.length : 0) +
                                (googleDriveFileIds != null ? googleDriveFileIds.length : 0));
            }

            log.info("✅ Respuesta enviada al frontend para pedido {}", npedidoGenerado);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ ERROR CRÍTICO en guardar pedido: {}", e.getMessage());
            log.error("   Tipo de excepción: {}", e.getClass().getName());
            log.error("   Stack trace:", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al guardar el pedido");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            errorResponse.put("timestamp", new Date());

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


    @GetMapping("/reactivar/{id}")
    public String reactivar(@PathVariable Long id, RedirectAttributes flash) {
        Optional<Pedido> optional = Optional.ofNullable(pedidoService.findOne(id));

        if (optional.isEmpty()) {
            flash.addFlashAttribute("error", "El pedido no existe.");
            return REDIRECTLISTAR;
        }
        Pedido pedido = optional.get();

        if (pedido.isActivo()) {
            flash.addFlashAttribute("info", "El pedido ya está activo.");
        } else {
            pedido.setActivo(true);  // ← Lo reactivamos
            pedidoService.save(pedido);
            flash.addFlashAttribute("success", "¡Pedido reactivado con éxito!");
        }

        // Redirigimos al detalle del cliente o lista general
        Long idCliente = pedido.getCliente() != null ? pedido.getCliente().getId() : null;
        return idCliente != null ? "redirect:/pedidos/listarPedidos"  : REDIRECTLISTAR;
    }

    /**
     *
     * @param id
     * @param flash
     * @return
     */
    @GetMapping("/eliminar/{id}")
    public String darDeBajaPedido(@PathVariable("id") Long id, RedirectAttributes flash) {
    log.info("Baja pedido {}", id);
        Optional<Pedido> optionalPedido = Optional.ofNullable(pedidoService.findOne(id));

        if (optionalPedido.isEmpty()) {
            flash.addFlashAttribute("error", "El pedido no existe.");
            return REDIRECTLISTAR;
        }

        Pedido pedido = optionalPedido.get();

        try {
            // Solo marcamos como inactivo (no borramos nada)
            pedido.darDeBaja();
            pedidoService.save(pedido);  // Guardamos el cambio

            flash.addFlashAttribute("success", "Pedido dado de baja correctamente.");

            Long idCliente = pedido.getCliente() != null ? pedido.getCliente().getId() : null;
            return idCliente != null ? "redirect:/pedidos/listarPedidos"  : REDIRECTLISTAR;

        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al dar de baja el pedido.");
            return REDIRECTLISTAR;
        }
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
    public String buscar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cliente", defaultValue = "") int id,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido,
            @RequestParam(name = "grupo", defaultValue = "") String grupo,
            @RequestParam(name = "pieza", defaultValue = "") String pieza,
            @RequestParam(name = "tipo", defaultValue = "") String tipo,
            @RequestParam(name = "ref", defaultValue = "") String ref,
            @RequestParam(name = "activo", defaultValue = "true") String activoStr,  // Recibimos como String
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable,
            Model model) {


        // Si no se proporciona fecha, usa null
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        Boolean activoFiltro = null;  // null = no filtrar → mostrar todos

        if (activoStr != null && !activoStr.isEmpty()) {
            if ("true".equalsIgnoreCase(activoStr)) {
                activoFiltro = true;
            } else if ("false".equalsIgnoreCase(activoStr)) {
                activoFiltro = false;
            }
            // Si es cualquier otro valor o vacío → queda null
        }

        // Paginación de las búsquedas totales
        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedido = pedidoService.buscarPedidos(id, tipoPedido, estado, grupo, pieza, tipo, ref, fechaDesdeDate, fechaHastaDate, activoFiltro,pageRequest);
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
    public String buscarFoto(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String servicios,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) String pieza,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String ref,
            @RequestParam(name = "activo", defaultValue = "") String activoStr,  // Recibimos como String
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable, Model model) throws JsonProcessingException {

        log.info("Buscar foto cliente id{}", id);
        // Conversión de fechas
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        // Búsqueda paginada
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedido = pedidoService.buscarPedidos(id, servicios, estado, grupo, pieza, tipo, ref, fechaDesdeDate, fechaHastaDate, Boolean.valueOf(activoStr), pageRequest);
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
        List<String> empleado = List.of("Anselmo");//TODO ESTO DEBE CAMBIAR PARA ACOGER LA PERSONA LOGUEADA O EL LISTADO DE PERSONAS EN EL LISTADO DE LAS EMPLEADOS CREADOS
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

    @GetMapping("/cloudinary-signature/{npedido}")
    public ResponseEntity<?> getCloudinarySignature(@PathVariable Long npedido) {
        try {
            log.info("📋 Solicitando firma de Cloudinary para pedido: {}", npedido);
            Map<String, Object> signature = cloudinaryService.generateUploadSignature(npedido);
            return ResponseEntity.ok(signature);
        } catch (Exception e) {
            log.error("❌ Error al generar firma: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/guardar-archivos/{npedido}")
    public ResponseEntity<?> guardarArchivos(
            @PathVariable Long npedido,
            @RequestBody Map<String, Object> request
    ) {
        try {
            log.info("💾 Guardando URLs de archivos desde Cloudinary para pedido: {}", npedido);
            
            Object urlsObj = request.get("urls");
            if (urlsObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No URLs provided"));
            }
            
            List<String> urls = (List<String>) urlsObj;
            int guardados = 0;
            
            for (String url : urls) {
                if (url != null && !url.isEmpty()) {
                    String fileName = "cloudinary_" + npedido + "_" + System.currentTimeMillis();
                    ArchivoAdjunto archivo = new ArchivoAdjunto(npedido, fileName, url);
                    archivoAdjuntoService.guardar(archivo);
                    guardados++;
                    log.info("✅ URL guardada: {}", url);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "archivosGuardados", guardados
            ));
        } catch (Exception e) {
            log.error("❌ Error al guardar archivos: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    /*
    Se utiliza para guardar los archivos en cloudinary y bd de google
     */
    @PostMapping("/subir-archivos/{npedido}")
    public ResponseEntity<?> subirArchivos(
            @PathVariable Long npedido,
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            @RequestParam(name = "googleDriveFileIds", required = false) String[] googleDriveFileIds,
            @RequestParam(name = "googleDriveToken", required = false) String googleDriveToken,
            String principal) {

        String userId = principal != null ? principal : null;

        int procesados = archivoSubidaService.procesarArchivos(npedido, files, googleDriveFileIds, googleDriveToken, userId);

        return ResponseEntity.ok(Map.of(
                "success", procesados > 0,
                "archivosSubidos", procesados,
                "npedido", npedido
        ));
    }


    /**
     * Descarga un archivo de Google Drive en streaming y lo sube directamente a Cloudinary.
     * Evita cargar el archivo completo en memoria (no usa byte[]).
     * Incluye medición de tiempos para depuración.
     *
     * @param fileId      ID del archivo en Google Drive
     * @param accessToken Token OAuth2 válido (Bearer)
     * @param npedido     Número de pedido para organizar en Cloudinary
     * @return URL segura del archivo subido a Cloudinary
     * @throws IOException Si hay error de red, autenticación o subida
     */
    private String downloadAndUploadToCloudinaryFromDrive(String fileId, String accessToken, Long npedido) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("📥 Iniciando descarga optimizada de Google Drive → Cloudinary - FileID: {}", fileId);

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Token de acceso vacío o nulo");
        }

        String apiUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";
        URL url = new URL(apiUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(20_000);   // 20 segundos para conexión (aumentado para handshake lento)
        connection.setReadTimeout(90_000);      // 1.5 minutos lectura (para archivos grandes)

        try {
            long connectStart = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long connectEnd = System.currentTimeMillis();

            log.debug("Tiempo de conexión + respuesta HTTP: {} ms - Código: {}",
                    (connectEnd - connectStart), responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                handleDriveError(responseCode, fileId);
            }

            // Obtener nombre original del archivo desde headers (mejor que timestamp)
            String disposition = connection.getHeaderField("Content-Disposition");
            String fileName = "gdrive_" + npedido + "_" + System.currentTimeMillis();

            if (disposition != null && disposition.contains("filename=")) {
                String originalName = disposition.split("filename=")[1]
                        .replaceAll("\"", "")
                        .replaceAll("[^a-zA-Z0-9._-]", "_"); // Sanitizar
                fileName = "gdrive_" + npedido + "_" + originalName;
            }

            log.info("🚀 Subiendo directamente a Cloudinary en streaming: {}", fileName);

            // STREAMING DIRECTO: Drive → Cloudinary sin memoria intermedia
            long uploadStart = System.currentTimeMillis();
            try (InputStream driveStream = connection.getInputStream()) {
                String cloudinaryUrl = cloudinaryService.uploadImage(driveStream, npedido, fileName);

                long uploadEnd = System.currentTimeMillis();
                log.info("✅ Drive → Cloudinary OK (streaming): {} bytes → {} (tiempo: {} ms)",
                        connection.getContentLengthLong(), cloudinaryUrl, (uploadEnd - uploadStart));

                return cloudinaryUrl;
            }

        } finally {
            connection.disconnect();
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("⏱️ Tiempo total descarga + subida para FileID {}: {} segundos", fileId, totalTime / 1000.0);
        }
    }

    /**
     * Manejo de errores HTTP comunes de Google Drive.
     */
    private void handleDriveError(int responseCode, String fileId) throws RuntimeException {
        switch (responseCode) {
            case 401 -> throw new RuntimeException("GDRIVE_401_TOKEN_EXPIRED");
            case 403 -> throw new RuntimeException("GDRIVE_403_ACCESS_DENIED");
            case 404 -> throw new RuntimeException("GDRIVE_404_NOT_FOUND");
            default -> throw new RuntimeException("GDRIVE_HTTP_ERROR_" + responseCode);
        }
    }
}
