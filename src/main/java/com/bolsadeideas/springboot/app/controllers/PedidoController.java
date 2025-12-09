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
    private RedisQueueProducer RedisQueueProducer;

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

    //VARIABLES
    static final String TITULO = "titulo";
    static final String ERROR = "error";
    boolean busquedaRealizada = false;
    //VISTAS
    static final String REDIRECTLISTAR = "redirect:/pedidolistar";
    static final String PEDIDOFORM = "/pedido/pedidoform";
    static final String CREARPEDIDO = "Crear Pedido";


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
        Page<Pedido> pedido = pedidoService.findAllPedidos(pageRequest);  // Llamada al servicio

        log.info("Listado de clientes pedidoService.findAllPedidos(pageRequest)" + pedido.getTotalElements());
        // Reparar el paginador eliminando el "/" en la URL
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Obtener todos los clientes para el filtro
        log.info("Listado de clientes clienteService.findAll()" + clienteService.findAll().toString());
        model.addAttribute("clientes", clienteService.findAll());

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
        tiposPorPieza.put("Pulsera", List.of("Pulsera"));
        tiposPorPieza.put("Pendientes", Arrays.asList("Pendiente", "Criollas", "1/2 Criolla", "Aretes", "Largos"));
        tiposPorPieza.put("Aro", Arrays.asList("Aro", "Aro Entorcillado", "Cierre caja", "Aros"));
        tiposPorPieza.put("Broche", List.of("Broche"));
        tiposPorPieza.put("Otros", List.of("Otros"));

        // Pasar al modelo
        model.addAttribute("tiposPorPieza", tiposPorPieza);

        // Otros datos necesarios
        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("estados", Arrays.asList("Finalizado", "Pendiente"));
        model.addAttribute(TITULO, "Listado de Pedidos");

        return "pedido/pedidolistar";
    }


    /// muestar ls fotos de los pedidos
    @RequestMapping(value = {"/listarFotosPedidos"}, method = RequestMethod.GET)
    public String listarFotos(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedido = pedidoService.findAll(pageRequest);


        // Reparar el paginador eliminando el "/" en la URL
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Obtener todos los clientes para el filtro
        model.addAttribute("clientes", clienteService.findAll());

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

        // Configuración del título y la página
        model.addAttribute(TITULO, "Listado de Pedidos");
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);

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


        log.info("entra en PedidoController");
        Pedido numeroPedido = pedidoService.obtenerUltimoNumeroPedido();
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);


        //List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO" ); EN LA VERSION NORRMAL SE PONDRA ESTE
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
//Pedido
        model.put("numeroPedido", numeroPedido.getNpedido() + 1);
        model.put("pedido", pedido);
// Proveedor
        model.put("proveedores", proveedorService.findAll());
        model.put(TITULO, CREARPEDIDO);
        return "pedido/pedidoform";
    }


    private void actualizarPedidoExistente(Pedido pedidoExistente, String observacion, String estado, String tipoPedido, String grupo, String pieza, Double peso, String horas, Double cobrado, Date fechaEntrega, Date fechaFinalizado, String empleado, String ref, RedirectAttributes flash) {
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
        pedidoExistente.setTipo(grupo); // incidencia #2 no se mostraba el tipo y pieza y no se guardaba
        pedidoExistente.setPieza(pieza); // incidencia #2 no se mostraba el tipo y pieza y no se guardaba

        try {
            //todo poner si tiene activado el envio de sms, que hay que implementar todavia en el registro del cliente
            if ("terminado".equalsIgnoreCase(pedidoExistente.getEstado()) && !pedidoExistente.getEnviadoSms()) {
                enviarSms(pedidoExistente);
            }
            pedidoService.save(pedidoExistente);
            flash.addFlashAttribute("info", "Pedido actualizado con éxito");
        } catch (Exception e) {
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
            pedidoService.save(pedido);
            flash.addFlashAttribute("info", "Pedido guardado con éxito");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar el pedido: " + e.getMessage());
        }
    }

    @PostMapping("/form")
    public ResponseEntity<?> guardar(
            @ModelAttribute @Valid Pedido pedido,
            BindingResult result, Model model,
            @RequestParam("observacion") String observacion,
            @RequestParam("estado") String estado,
            @RequestParam("tipoPedido") String tipoPedido,
            @RequestParam("grupo") String grupo,
            @RequestParam("pieza") String pieza,
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

        Long npedido = pedido.getNpedido();
        Double pesoDouble = parsePeso(peso);
        Double cobradoDouble = parseCobrado(cobrado);
        String horasSaneadas = parseHoras(horas);
        sanitizeClienteNombre(pedido);

        // -------------------------------------------------------------------------
        // 1. Lógica de actualización de pedido existente
        // -------------------------------------------------------------------------
        if (npedido != null && npedido > 0) {
            Pedido pedidoExistente = pedidoService.findOne(npedido);
            if (pedidoExistente != null) {
                // Actualizamos el pedido existente
                actualizarPedidoExistente(pedidoExistente, observacion, estado, tipoPedido, grupo, pieza,
                        pesoDouble, horasSaneadas, cobradoDouble, fechaEntrega,
                        fechaFinalizado, empleado, ref, flash);

                // Subida de archivos si existen
                if (files != null && files.length > 0) {
                    subirYGuardarArchivos(files, npedido, pedidoExistente.getCliente().getId(), flash);
                }

                // Finalizamos sesión y devolvemos respuesta AJAX con redirección
                status.setComplete();
                String redirectUrl = "/pedidos/form/" + pedidoExistente.getCliente().getId();  // Redirigir al formulario del cliente
                return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl, "info", "Pedido actualizado con éxito"));
            } else {
                log.warn("Pedido con npedido={} no encontrado. Se creará uno nuevo.", npedido);
            }
        }

        // -------------------------------------------------------------------------
        // 2. Lógica de creación de nuevo pedido
        // -------------------------------------------------------------------------
        guardarNuevoPedido(pedido, flash);

        // Subida de archivos si existen
        if (files != null && files.length > 0) {
            subirYGuardarArchivos(files, pedido.getNpedido(), pedido.getCliente().getId(), flash);
        }

        // Finalizamos sesión y devolvemos respuesta AJAX con redirección
        status.setComplete();
        String redirectUrl = "/pedidos/form/" + pedido.getCliente().getId();  // Siempre redirige al formulario del cliente, sea nuevo o actualizado
        log.info("Nuevo pedido creado con npedido={}", pedido.getCliente().getId());

        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl, "info", "Pedido y archivos guardados con éxito"));
    }

//se ha cambado por esta version


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
    if (fotos != null && fotos.length > 0) {
        List<ArchivoAdjunto> archivosBatch = new ArrayList<>();

        for (MultipartFile foto : fotos) {
            if (!foto.isEmpty()) {
                try {
                    // *** Lógica de subida a Cloudinary ***
                    byte[] imageBytes = foto.getBytes();
                    String fileName = "pedido_" + npedido + "_" + System.currentTimeMillis();  // Nombre único para cada archivo

                    // Subir la imagen a Cloudinary
                    String imageUrl = cloudinaryService.uploadImage(imageBytes, npedido, fileName);  // Usamos los parámetros adecuados
                    String publicId = fileName;  // El publicId ahora lo podemos obtener del mismo fileName

                    // Enviar los metadatos a Redis para ser procesados en segundo plano
                    redisTestService.testConnection();  // Verifica que la conexión de Redis esté activa
                    String mensaje = npedido + ";" + foto.getOriginalFilename() + ";" + publicId;

                    // **Guardar el mensaje en Redis**
                    redisTemplate.opsForValue().set("testKey", mensaje);  // Guardamos el mensaje en Redis bajo la clave 'testKey'
                    log.info("Mensaje enviado a Redis: {}", mensaje);  // Verifica que el mensaje se ha guardado

                    // Verificar que el valor se guarda correctamente en Redis
                    String testKeyValue = redisTemplate.opsForValue().get("testKey");
                    log.info("Valor de testKey en Redis: {}", testKeyValue);  // Debería mostrar el valor guardado

                    // **Enviar el mensaje para ser procesado en segundo plano**
                    RedisQueueProducer.sendMessage(mensaje);

                    flash.addFlashAttribute("info", "Archivos subidos con éxito.");
                } catch (IOException e) {
                    flash.addFlashAttribute("error", "Error al subir: " + foto.getOriginalFilename());
                    e.printStackTrace();
                }
            }
        }
    }
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


}
