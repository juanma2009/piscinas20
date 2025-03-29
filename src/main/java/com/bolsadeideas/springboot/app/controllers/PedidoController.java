package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.apigoogledrice.GoogleDriveService;
import com.bolsadeideas.springboot.app.apisms.AppSms;
import com.bolsadeideas.springboot.app.models.entity.*;
import com.bolsadeideas.springboot.app.models.service.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private CloudinaryService cloudinaryService;

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

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .body(resource);
        } catch (IOException e) {
            // En caso de error, retornar 404
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar la imagen desde Cloudinary", e);
        }
    }


    /*

    @GetMapping("/ver/fotos/{fileId}")
    public ResponseEntity<ByteArrayResource> verFoto(@PathVariable String fileId) {
        try {
            // Descargar el archivo desde Google Drive
            InputStream in = googleDriveService.downloadFile(fileId);
            byte[] data = IOUtils.toByteArray(in);
            ByteArrayResource resource = new ByteArrayResource(data);

            // Si conoces el mime type (por ejemplo "image/jpeg") lo puedes especificar,
            // o bien detectarlo dinámicamente si lo tienes almacenado.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(data.length)
                    .body(resource);
        } catch (IOException e) {
            // En caso de error, puedes retornar un 404 o el código de error que consideres apropiado
            return ResponseEntity.notFound().build();
        }
    }
*/
    @RequestMapping(value = {"/listarPedidos"}, method = RequestMethod.GET)
    public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        Pageable pageRequest = PageRequest.of(page, 5);
        Page<Pedido> pedido = pedidoService.findAll(pageRequest);

        // Reparar el paginador eliminando el "/" en la URL
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Obtener todos los clientes para el filtro
        model.addAttribute("clientes", clienteService.findAll());

        // Listar estados posibles del pedido
        List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO");
        model.addAttribute("estados", estados);

        // Tipos de pedido
        List<String> tipoPedidos = Arrays.asList("NUEVO DISEÑO", "REPARACION", "REPLICA", "MODIFICADO");
        model.addAttribute("tipoPedidos", tipoPedidos);

        // Grupos de metales
        List<String> grupo = Arrays.asList("ORO", "PLATA", "DIAMANTES", "ORO BLANCO", "ORO ROJO", "ORO ROSA", "ORO AMARILLO");
        model.addAttribute("grupo", grupo);

        // Subgrupos (tipos de joyas)
        List<String> subgrupo = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE", "PULSERAS", "SELLO");
        model.addAttribute("subgrupo", subgrupo);

        /*
        // Obtener imágenes de cada pedido utilizando el servicio de Cloudinary
        Map<Long, String> imagenesPedidos = new HashMap<>();
        for (Pedido p : pedido.getContent()) {
            List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());
            if (!archivos.isEmpty()) {
                // Aquí obtenemos el ID público de Cloudinary de cada archivo adjunto
                String publicId = archivos.get(0).getUrlCloudinary(); // Obtener el publicId del archivo
                try {
                    // Usamos el servicio Cloudinary para obtener la URL de la imagen
                    String imageUrl = cloudinaryService.getImageUrl(publicId);
                    imagenesPedidos.put(p.getNpedido(), imageUrl); // Asignamos la URL de la imagen al pedido
                } catch (Exception e) {
                    // En caso de error, se asigna una imagen por defecto
                    imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
                }
            } else {
                // Si no hay imágenes adjuntas, se asigna una imagen por defecto
                imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
            }
        }

        // Agregar la lista de imágenes al modelo
        model.addAttribute("imagenesPedidos", imagenesPedidos);
*/
        // Configuración del título y la página
        model.addAttribute(TITULO, "Listado de Pedidos");
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);

        return "pedido/pedidolistar";
    }

 /// muestar ls fotos de lso peidos
 ///
 @RequestMapping(value = {"/listarFotosPedidos"}, method = RequestMethod.GET)
 public String listarFotos(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
     Pageable pageRequest = PageRequest.of(page,Integer.MAX_VALUE );
     Page<Pedido> pedido = pedidoService.findAll(pageRequest);


     // Reparar el paginador eliminando el "/" en la URL
     PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

     // Obtener todos los clientes para el filtro
     model.addAttribute("clientes", clienteService.findAll());

     // Listar estados posibles del pedido
     List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO");
     model.addAttribute("estados", estados);

     // Tipos de pedido
     List<String> tipoPedidos = Arrays.asList("NUEVO DISEÑO", "REPARACION", "REPLICA", "MODIFICADO");
     model.addAttribute("tipoPedidos", tipoPedidos);

     // Grupos de metales
     List<String> grupo = Arrays.asList("ORO", "PLATA", "DIAMANTES", "ORO BLANCO", "ORO ROJO", "ORO ROSA", "ORO AMARILLO");
     model.addAttribute("grupo", grupo);

     // Subgrupos (tipos de joyas)
     List<String> subgrupo = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE", "PULSERAS", "SELLO");
     model.addAttribute("subgrupo", subgrupo);
/*
     if(!busquedaRealizada){
     // Obtener imágenes de cada pedido utilizando el servicio de Cloudinary
     Map<Long, String> imagenesPedidos = new HashMap<>();
     for (Pedido p : pedido.getContent()) {
         List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());
         if (!archivos.isEmpty()) {
             // Aquí obtenemos el ID público de Cloudinary de cada archivo adjunto
             String publicId = archivos.get(0).getUrlCloudinary(); // Obtener el publicId del archivo
             try {
                 // Usamos el servicio Cloudinary para obtener la URL de la imagen
                 String imageUrl = cloudinaryService.getImageUrl(publicId);
                 imagenesPedidos.put(p.getNpedido(), imageUrl); // Asignamos la URL de la imagen al pedido
             } catch (Exception e) {
                 // En caso de error, se asigna una imagen por defecto
                 imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
             }
         } else {
             // Si no hay imágenes adjuntas, se asigna una imagen por defecto
             imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
         }
     }

        // Agregar la lista de imágenes al modelo
         model.addAttribute("imagenesPedidos", imagenesPedidos);
     }
*/
     // Configuración del título y la página
     model.addAttribute(TITULO, "Listado de Pedidos");
     model.addAttribute("pedido", pedido);
     model.addAttribute("page", pageRender);

     return "pedido/pedidofotolistar";
 }




    @GetMapping("/ver/{id}")
    public String ver(@PathVariable(value = "id") Long id,
                      Model model,
                      RedirectAttributes flash) {
        Pedido pedido = clienteService.findPedidoById(id);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe en la base de datos!");
            return "redirect:/listar";
        }

        model.addAttribute("pedido", pedido);
        // Suponiendo que ya has obtenido las URLs de las imágenes en el método cargarImagenes
        // O bien puedes obtenerlas en este mismo método a través del servicio
      //  List<String> fotos = archivoAdjuntoService.obtenerUrlsFotos(pedido.getNpedido());
      //  model.addAttribute("fotos", fotos);
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

    /*

    @GetMapping("/cargarImagenes/{id}")
    @ResponseBody
    public List<String> cargarImagenes(@PathVariable(value = "id") Long id) {
        List<String> urls = new ArrayList<>();

        // Obtener los archivos adjuntos asociados al pedido
        List<ArchivoAdjunto> archivosAdjuntos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(id);

        for (ArchivoAdjunto archivo : archivosAdjuntos) {
            if (archivo.getGoogleDriveFileId() != null) {
                // Construir la URL pública para cada archivo
                String url = "" + archivo.getGoogleDriveFileId();
                urls.add(url);
            } else {
                log.warn("El archivo con nombre " + archivo.getNombre() + " no tiene un Google Drive File ID asociado.");
            }
        }

        return urls;
    }

*/

    /**
     * Crear los Pedidos para los clientes
     *
     * @param clienteId
     * @param model
     * @param flash
     * @return
     */
    @GetMapping("/form/{clienteId}")
    public String crear(@PathVariable(value = "clienteId") Long clienteId,
                        Map<String, Object> model,
                        RedirectAttributes flash) {
        Cliente cliente = clienteService.findOne(clienteId);
        if (cliente == null) {
            flash.addFlashAttribute(ERROR, "El cliente no existe en la base de datos");
            return REDIRECTLISTAR;
        }


        log.info("entra en PedidoController");
        Pedido numeroPedido =pedidoService.obtenerUltimoNumeroPedido();
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);

        log.info(numeroPedido);
        List<String> tipoPedido = Arrays.asList("NUEVO DISEÑO","REPARACION","REPLICA","MODIFICADO" );
        model.put("tipoPedido", tipoPedido);
        //List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO" ); EN LA VERSION NORRMAL SE PONDRA ESTE
        List<String> estados = Arrays.asList("FINALIZADO","PENDIENTE");
        model.put("estados", estados);
        List<String> grupo = Arrays.asList("ORO", "PLATA", "DIAMANTES","ORO BLANCO","ORO ROJO","ORO ROSA","ORO AMARILLO");
        model.put("grupo", grupo);
        List<String> subgrupo = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE","PULSERAS","SELLO");
        model.put("subgrupo", subgrupo);

        List<String> empleado = Arrays.asList("JUAN", "PEDRO", "MARIA","JUANA","JUANITA");
        model.put("empleado", empleado);

        model.put("numeroPedido", numeroPedido.getNpedido()+1);
        model.put("pedido", pedido);
        model.put("proveedores", proveedorService.findAll());
        model.put(TITULO, CREARPEDIDO);
        return "pedido/pedidoform";
    }


    @PostMapping("/form")
    public String guardar(@ModelAttribute @Valid Pedido pedido,
                          BindingResult result,
                          Model model,
                          @RequestParam("npedido") Long npedido,
                          @RequestParam("observacion") String observacion,
                          @RequestParam("estado") String estado,
                          @RequestParam("tipoPedido") String tipoPedido,
                          @RequestParam("grupo") String grupo,
                          @RequestParam("subgrupo") String subgrupo,
                          @RequestParam(name ="peso",required = false) String peso,
                          @RequestParam(name ="horas",required = false) String horas,
                          @RequestParam(name ="cobrado",required = false) String cobrado,
                          @RequestParam(name = "fecha",required = false) Date fechaFinalizado,
                          @RequestParam(name = "empleado",required = false) String empleado,
                          @RequestParam(name = "ref",required = false) String ref,
                          RedirectAttributes flash,
                          SessionStatus status,
                          @RequestParam("fileNamesJSON") String fileNamesJSON) {
        if (result.hasErrors()) {
            model.addAttribute(TITULO, CREARPEDIDO);
            return PEDIDOFORM;
        }

        Pedido pedidoExistente = pedidoService.findOne(npedido);
        if (pedidoExistente != null) {
            actualizarPedidoExistente(pedidoExistente, observacion, estado, tipoPedido,grupo,subgrupo, Double.valueOf(peso),horas,cobrado,fechaFinalizado,empleado,ref, flash);
        } else {
            guardarNuevoPedido(pedido, flash);
        }

        status.setComplete();
        return "redirect:/pedidos/form/" + pedido.getCliente().getId();
    }
        private void actualizarPedidoExistente(Pedido pedidoExistente, String observacion, String estado, String tipoPedido, String grupo, String subgrupo, Double peso, String horas, String cobrado, Date fecha, String empleado, String ref, RedirectAttributes flash) {
        pedidoExistente.setObservacion(observacion);
        pedidoExistente.setEstado(estado);
        pedidoExistente.setTipoPedido(tipoPedido);
        pedidoExistente.setGrupo(grupo);
        pedidoExistente.setSubgrupo(subgrupo);
        pedidoExistente.setPeso(peso);
        pedidoExistente.setHoras(horas);
        pedidoExistente.setCobrado(cobrado);
        pedidoExistente.setFechaFinalizdo(fecha);
        pedidoExistente.setRef(ref);
        pedidoExistente.setEmpleado(empleado);

        try {
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

    @PostMapping("/guardarFotos")
    @ResponseStatus(HttpStatus.OK)
    public void guardarFotos(@Valid ArchivoAdjunto archivoAdjunto,
                             @RequestParam("npedido") String npedido,
                             @RequestParam("files") MultipartFile[] fotos,
                             RedirectAttributes flash) {
        System.out.println("npedido recibido: " + npedido);
        String pedidoId = npedido.replaceAll("[^0-9]", "").trim();

        if (fotos != null && fotos.length > 0) {
            for (MultipartFile foto : fotos) {
                if (!foto.isEmpty()) {
                    try {
                        // Convertir el archivo MultipartFile a byte array
                        byte[] imageBytes = foto.getBytes();

                        // Subir archivo a Cloudinary
                        String fileName = "foto_" + System.currentTimeMillis();
                        String imageUrl = cloudinaryService.uploadImage(imageBytes, fileName);

                        String publicId = imageUrl.split("/v\\d+/")[1].split("\\.")[0];

                        // Guardar la referencia del archivo en la base de datos
                        ArchivoAdjunto nuevoArchivo = new ArchivoAdjunto();
                        nuevoArchivo.setNombre(foto.getOriginalFilename());
                        nuevoArchivo.setUrlCloudinary(publicId);
                        nuevoArchivo.setPedidoAdjunto(Long.valueOf(pedidoId));
                        archivoAdjuntoService.guardar(nuevoArchivo);

                    } catch (IOException e) {
                        flash.addFlashAttribute("error", "Error al subir el archivo: " + foto.getOriginalFilename());
                        e.printStackTrace();
                    }
                }
            }
            flash.addFlashAttribute("info", "Archivos subidos con éxito a Cloudinary.");
        } else {
            flash.addFlashAttribute("error", "No se seleccionaron archivos para subir.");
        }
    }




    /**
     * Al guardar las fotos se deben guardar en archvivos adjuntos con el numero de pedido y nombre de la foto
     * por otra parte se debe guardar en la carpeta de fotos del sistema operativo el archivo para poder ser cargado poesteriormete
     * @param npedido
     * @param foto
     * @param flash
     * @return
     */
    /*
    @PostMapping("/guardarFotos")
    @ResponseStatus(HttpStatus.OK) // Indica que la respuesta será 200 OK sin vista
    public void guardarFotos(@Valid ArchivoAdjunto archivoAdjunto,
                             @RequestParam("npedido") String npedido,
                             @RequestParam("files") MultipartFile foto,
                             RedirectAttributes flash) {
        System.out.println("npedido recibido: " + npedido);
        String pedidoId = npedido.replaceAll("[^0-9]", "").trim();
        if (!foto.isEmpty()) {
            try {
                // Subir archivo a Google Drive
                String fileId = googleDriveService.uploadFileToFolder(foto);

                // Guardar la referencia del archivo en la base de datos
                archivoAdjunto.setNombre(foto.getOriginalFilename());
                archivoAdjunto.setGoogleDriveFileId(fileId);
                archivoAdjunto.setPedidoAdjunto(Long.valueOf(pedidoId));
                archivoAdjuntoService.guardar(archivoAdjunto);

                flash.addFlashAttribute("info", "Archivo subido con éxito a Google Drive: " + foto.getOriginalFilename());
            } catch (IOException e) {
                flash.addFlashAttribute("error", "Error al subir el archivo a Google Drive: " + foto.getOriginalFilename());
                e.printStackTrace();
            }
        } else {
            flash.addFlashAttribute("error", "No se seleccionó ningún archivo para subir.");
        }
    }
*/


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
     * @param cliente
     * @param pageable
     * @param model
     * @return
     */
    @PostMapping("/buscar")
    public String buscar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cliente", defaultValue = "") String cliente,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido,
            @RequestParam(name = "grupo", defaultValue = "") String grupo,
            @RequestParam(name = "subgrupo", defaultValue = "") String subgrupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable, Model model) {


        // Si no se proporciona fecha, usa null
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        // Paginación de las búsquedas totales
        Pageable pageRequest = PageRequest.of(page, 6);
        Page<Pedido> pedido = pedidoService.buscarPedidos(cliente, tipoPedido,estado, grupo, subgrupo, fechaDesdeDate, fechaHastaDate, pageRequest);
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Agregar la lista de imágenes de los pedidos
        Map<Long, String> imagenesPedidos = new HashMap<>();
        for (Pedido p : pedido.getContent()) {
            List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());
            if (!archivos.isEmpty()) {
                String publicId = archivos.get(0).getUrlCloudinary();
                try {
                    String imageUrl = cloudinaryService.getImageUrl(publicId);
                    imagenesPedidos.put(p.getNpedido(), imageUrl);
                } catch (Exception e) {
                    imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
                }
            } else {
                imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
            }
        }

        model.addAttribute("imagenesPedidos", imagenesPedidos);

        // Datos estáticos (estados, grupos, subgrupos, etc.)
        List<String> tipoPedidos = Arrays.asList("NUEVO DISEÑO", "REPARACION", "REPLICA", "MODIFICADO");
        model.addAttribute("tipoPedidos", tipoPedidos);

        // Filtros disponibles
        List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO","FINALIZADO");
        model.addAttribute("estados", estados);
        List<String> grupos = Arrays.asList("ORO", "PLATA", "DIAMANTES", "ORO BLANCO", "ORO ROJO", "ORO ROSA", "ORO AMARILLO");
        model.addAttribute("grupo", grupos);
        List<String> subgrupos = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE", "PULSERAS", "SELLO");
        model.addAttribute("subgrupo", subgrupos);

        // Obtener los clientes para el filtro
        model.addAttribute("clientes", clienteService.findAll());

        // Filtros seleccionados (si no se pasa nada, se deja vacío o con valor predeterminado)
        model.addAttribute("clienteSeleccionado", cliente);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("grupoSeleccionado", grupo);
        model.addAttribute("tipoPedidoSeleccionado", tipoPedido);
        model.addAttribute("subgrupoSeleccionado", subgrupo);
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
            @RequestParam(name = "cliente", defaultValue = "") String cliente,
            @RequestParam(name = "estado", defaultValue = "") String estado,
            @RequestParam(name = "tipoPedido", defaultValue = "") String tipoPedido,
            @RequestParam(name = "grupo", defaultValue = "") String grupo,
            @RequestParam(name = "subgrupo", defaultValue = "") String subgrupo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable pageable, Model model) {


        // Si no se proporciona fecha, usa null
        Date fechaDesdeDate = (fechaDesde != null) ? Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date fechaHastaDate = (fechaHasta != null) ? Date.from(fechaHasta.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        // Paginación de las búsquedas totales
        Pageable pageRequest = PageRequest.of(page, Integer.MAX_VALUE);
        Page<Pedido> pedido = pedidoService.buscarPedidos(cliente, tipoPedido,estado, grupo, subgrupo, fechaDesdeDate, fechaHastaDate, pageRequest);
        PageRender<Pedido> pageRender = new PageRender<>("listarPedidos", pedido);

        // Agregar la lista de imágenes de los pedidos
        Map<Long, String> imagenesPedidos = new HashMap<>();
        for (Pedido p : pedido.getContent()) {
            List<ArchivoAdjunto> archivos = archivoAdjuntoService.findArchivosAdjuntosByPedidoId(p.getNpedido());
            if (!archivos.isEmpty()) {
                String publicId = archivos.get(0).getUrlCloudinary();
                try {
                    String imageUrl = cloudinaryService.getImageUrl(publicId);
                    imagenesPedidos.put(p.getNpedido(), imageUrl);
                } catch (Exception e) {
                    imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
                }
            } else {
                imagenesPedidos.put(p.getNpedido(), "/img/default.jpg");
            }
        }
        model.addAttribute("busquedaRealizada", busquedaRealizada=true);
        model.addAttribute("imagenesPedidos", imagenesPedidos);

        // Datos estáticos (estados, grupos, subgrupos, etc.)
        List<String> tipoPedidos = Arrays.asList("NUEVO DISEÑO", "REPARACION", "REPLICA", "MODIFICADO");
        model.addAttribute("tipoPedidos", tipoPedidos);

        // Filtros disponibles
        List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO","FINALIZADO");
        model.addAttribute("estados", estados);
        List<String> grupos = Arrays.asList("ORO", "PLATA", "DIAMANTES", "ORO BLANCO", "ORO ROJO", "ORO ROSA", "ORO AMARILLO");
        model.addAttribute("grupo", grupos);
        List<String> subgrupos = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE", "PULSERAS", "SELLO");
        model.addAttribute("subgrupo", subgrupos);

        // Obtener los clientes para el filtro
        model.addAttribute("clientes", clienteService.findAll());

        // Filtros seleccionados (si no se pasa nada, se deja vacío o con valor predeterminado)
        model.addAttribute("clienteSeleccionado", cliente);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("grupoSeleccionado", grupo);
        model.addAttribute("tipoPedidoSeleccionado", tipoPedido);
        model.addAttribute("subgrupoSeleccionado", subgrupo);
        model.addAttribute("fechaDesdeSeleccionada", fechaDesde != null ? fechaDesde : "");
        model.addAttribute("fechaHastaSeleccionada", fechaHasta != null ? fechaHasta : "");

        // Otros valores
        model.addAttribute("TITULO", "Lista de Pedidos Encontradas ");
        model.addAttribute("textoR", "Resultados Encontrados: ");
        model.addAttribute("pedido", pedido);
        model.addAttribute("page", pageRender);
        model.addAttribute("countProveedor", proveedorService.count());

        return "pedido/pedidofotolistar";
    }

    //muetra ls fotos de lso pedidos

    @GetMapping("/eliminarFoto/{fileId}")
    public String eliminarFoto(@PathVariable("fileId") String fileId,
                               @RequestParam("npedido") String pedidoId,
                               RedirectAttributes flash) {
        log.info("Llegan del front: pedidoId=" + pedidoId + ", fileId=" + fileId);

        // Buscar el archivo en la base de datos relacionado con este pedido
        ArchivoAdjunto archivo = archivoAdjuntoService.findArchivosAdjuntosByPedidoIdOne(fileId, Long.valueOf(pedidoId));

        log.info("Archivo encontrado: " + archivo);

        if (archivo != null) {
            try {
                // Eliminar de Cloudinary
                String deleteResult = cloudinaryService.deleteImage(archivo.getUrlCloudinary()); // Aquí debes cambiar el método a obtener el publicId de Cloudinary

                if ("ok".equals(deleteResult)) {
                    // Eliminar la referencia en la base de datos
                    archivoAdjuntoService.eliminarArchivoAdjunto(archivo);
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


    /*
    @GetMapping("/eliminarFoto/{fileId}")
    public String eliminarFoto(@PathVariable("fileId") String fileId,
                               @RequestParam("npedido") String pedidoId,
                               RedirectAttributes flash) {
log.info("llegan del front"+pedidoId+fileId);
        // Buscar el pedido en la base de datos


        // Buscar el archivo en la base de datos relacionado con este pedido
        ArchivoAdjunto archivo = archivoAdjuntoService.findArchivosAdjuntosByPedidoIdOne(fileId, pedidoId);

        log.info(archivo);
        if (archivo != null) {
            try {
                // Eliminar de Google Drive
                googleDriveService.deleteFile(archivo.getGoogleDriveFileId());

                // Eliminar la referencia en la base de datos
                archivoAdjuntoService.eliminarArchivoAdjunto(archivo);

                flash.addFlashAttribute("info", "Foto eliminada con éxito de Google Drive y del pedido!");
            } catch (Exception e) {
                flash.addFlashAttribute("error", "Error al eliminar la foto: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            flash.addFlashAttribute("error", "No se encontró la foto asociada.");
        }

        return "redirect:/pedidos/form/" + pedidoId;
    }
*/



    @RequestMapping(value = "/formEditar/{id}")
    public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

        Pedido pedido = null;

        if (id > 0) {
            pedido = pedidoService.findOne(id);
            if (pedido == null) {
                flash.addFlashAttribute("error", "El ID del Pedido no existe en la BBDD!");
                return "redirect:pedido/listar";
            }
        } else {
            flash.addFlashAttribute("error", "El ID del Pedido no puede ser cero!");
            return "redirect:pedido/listar";
        }

        List<String> tipoPedido = Arrays.asList("NUEVO DISEÑO","REPARACION","REPLICA","MODIFICADO" );
        model.put("tipoPedido", tipoPedido);
        //List<String> estados = Arrays.asList("PENDIENTE", "REALIZANDO", "TERMINADO" ); EN LA VERSION NORRMAL SE PONDRA ESTE
        List<String> estados = Arrays.asList("FINALIZADO","PENDIENTE");
        model.put("estados", estados);
        List<String> grupo = Arrays.asList("ORO", "PLATA", "DIAMANTES","ORO BLANCO","ORO ROJO","ORO ROSA","ORO AMARILLO");
        model.put("grupo", grupo);
        List<String> subgrupo = Arrays.asList("ANILLO", "COLGANTE", "PENDIENTE","PULSERAS","SELLO");
        model.put("subgrupo", subgrupo);

        List<String> empleado = Arrays.asList("JUAN", "PEDRO", "MARIA","JUANA","JUANITA");
        model.put("empleado", empleado);

        model.put("tipoPedido", tipoPedido);
        model.put("pedido", pedido);
        model.put("titulo", "Editar Pedido");
        return "pedido/pedidoform";
    }

    @PostMapping("/report")
    public ResponseEntity<?> generateReport(HttpServletResponse response,
                                            @RequestParam(name = "cliente") String cliente,
                                            @RequestParam(name = "estado") String estado){
        log.info("cliente: " + cliente);
        log.info("estado: " + estado);
        try {
            // Generar el informe
            JasperPrint jasperPrint = pedidoService.generateJasperPrint(cliente,estado);

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

}
