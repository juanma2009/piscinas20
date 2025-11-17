package com.bolsadeideas.springboot.app.models.service;


import com.bolsadeideas.springboot.app.models.dao.PedidoDao;
import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.entity.Pedido;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

	@Autowired
	private IClienteService clienteService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private PedidoDao pedidoDao;

	Logger log = Logger.getLogger(PedidoServiceImpl.class.getName());

	@Override
	@Transactional(readOnly=true)
	public Iterable<Pedido> findAll() {
		return  pedidoDao.findAll();
	}

	@Override
	public Map<String, Double> totalFacturadoPorMesAnios(Long clienteId, int anio) {
		return Map.of();
	}

	@Transactional
	public Pedido save(Pedido  pedido) {

		return pedidoDao.save(pedido);
	}

	@Override
	@Transactional(readOnly=true)
	public Pedido findOne(Long id) {
		return pedidoDao.findById(id).orElse(null);
	}
	@Override
	@Transactional
	public void delete(Long id) {
		pedidoDao.deleteById(id);
	}

	@Override
	@Transactional(readOnly=true)
	public long count() {			
		return pedidoDao.count();
	}

	@Override
	public Page<Pedido> findByCliente(String cliente, String estado, Pageable pageable) {
		return null;
	}

	public Page<Pedido> buscarPedidos(Integer id, String tipoPedido, String estado, String grupo,
									  String pieza, String tipo, String ref, Date fechaDesde, Date fechaHasta,
									  Pageable pageable) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Pedido> cq = cb.createQuery(Pedido.class);
		Root<Pedido> pedido = cq.from(Pedido.class);
		Join<Pedido, Cliente> clienteJoin = pedido.join("cliente", JoinType.LEFT);

		List<Predicate> predicates = new ArrayList<>();
		log.info("id del cleinte"+id);
		if (id != null && id != 0) {
			predicates.add(cb.equal(clienteJoin.get("id"), id));
		}
		if (tipoPedido != null && !tipoPedido.isEmpty()) {
			predicates.add(cb.equal(pedido.get("tipoPedido"), tipoPedido));
		}
		if (estado != null && !estado.isEmpty()) {
			predicates.add(cb.like(cb.lower(pedido.get("estado")), "%" + estado.toLowerCase() + "%"));
		}
		if (grupo != null && !grupo.isEmpty()) {
			predicates.add(cb.equal(pedido.get("grupo"), grupo));
		}
		if (pieza != null && !pieza.isEmpty()) {
			predicates.add(cb.equal(pedido.get("pieza"), pieza));
		}
		if (tipo != null && !tipo.isEmpty()) {
			predicates.add(cb.equal(pedido.get("tipo"), tipo));
		}
		if (ref != null && !ref.isEmpty()) {
			predicates.add(cb.equal(pedido.get("ref"), ref));
		}
		if (fechaDesde != null) {
			predicates.add(cb.greaterThanOrEqualTo(pedido.get("fechaEntrega"), fechaDesde));
		}
		if (fechaHasta != null) {
			predicates.add(cb.lessThanOrEqualTo(pedido.get("fechaEntrega"), fechaHasta));
		}

		cq.where(predicates.toArray(new Predicate[0]));

		// Añadir ordenación según pageable.getSort()
		if (pageable.getSort() != null) {
			List<Order> orders = new ArrayList<>();
			pageable.getSort().forEach(order -> {
				Path<?> path = pedido.get(order.getProperty());
				orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
			});
			cq.orderBy(orders);
		}

		TypedQuery<Pedido> query = entityManager.createQuery(cq);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		List<Pedido> pedidos = query.getResultList();

		// COUNT query (con el join al cliente para que el filtro por cliente funcione también)
		CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
		Root<Pedido> countRoot = countQuery.from(Pedido.class);
		Join<Pedido, Cliente> countClienteJoin = countRoot.join("cliente", JoinType.LEFT);

		List<Predicate> countPredicates = new ArrayList<>();

		if (id != null && id != 0) {
			countPredicates.add(cb.equal(countClienteJoin.get("id"), id));
		}
		if (tipoPedido != null && !tipoPedido.isEmpty()) {
			countPredicates.add(cb.equal(countRoot.get("tipoPedido"), tipoPedido));
		}
		if (estado != null && !estado.isEmpty()) {
			countPredicates.add(cb.like(cb.lower(countRoot.get("estado")), "%" + estado.toLowerCase() + "%"));
		}
		if (grupo != null && !grupo.isEmpty()) {
			countPredicates.add(cb.equal(countRoot.get("grupo"), grupo));
		}
		if (pieza != null && !pieza.isEmpty()) {
			countPredicates.add(cb.equal(countRoot.get("pieza"), pieza));
		}
		if (tipo != null && !tipo.isEmpty()) {
			countPredicates.add(cb.equal(countRoot.get("tipo"), tipo));
		}
		if (ref != null && !ref.isEmpty()) {
			countPredicates.add(cb.equal(countRoot.get("ref"), ref));
		}
		if (fechaDesde != null) {
			countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("fechaEntrega"), fechaDesde));
		}
		if (fechaHasta != null) {
			countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("fechaEntrega"), fechaHasta));
		}

		countQuery.select(cb.count(countRoot));
		countQuery.where(countPredicates.toArray(new Predicate[0]));
		Long totalRegistros = entityManager.createQuery(countQuery).getSingleResult();

		return new PageImpl<>(pedidos, pageable, totalRegistros);
	}

	@Override
	public Pedido obtenerUltimoNumeroPedido() {

		Pedido pedido = pedidoDao.findTopByOrderByNpedidoDesc();
		if(pedido == null) {
			pedido = new Pedido();
			pedido.setNpedido(0L);
		}
		return pedido;
	}

	@Override
	public List<Pedido> findAllPedidos() {
		return List.of();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Pedido> findAllPedidos(Pageable pageable) {
		Page<Pedido> pedidos = pedidoDao.findAllWithCliente(pageable);

		// Diagnóstico: verificar que el cliente está cargado
		pedidos.getContent().forEach(p -> {
			if (p.getCliente() != null) {
				System.out.println("Cliente cargado: " + p.getCliente().getNombre()); // Verifica que no sea null
			} else {
				System.out.println("Cliente no cargado");
			}
		});

		return pedidos;
	}




	public Page<Pedido> findPedidoByIdClienteAndFinalizados(Long idCliente, Pageable pageable) {
		return  pedidoDao.findPedidosByIdClienteAndTerminado(idCliente, pageable);
	}

	/**
	 * 	todo arreglar para admirtir paginacion
	 * Obtiene una página de pedidos asociados a un cliente específico.
	 */
	@Override
	public Page<Pedido> findAllByCliente(Long idcliente, Pageable pageable) {
		return  pedidoDao.findPedidoById(idcliente, pageable);
	}


	//----------


	public Page<Pedido> getPedidosById(Long id, Pageable pageable) {
		// Obtener los pedidos paginados
		Page<Pedido> pedidos = pedidoDao.findPedidoById(id, pageable);

		// Obtener el conteo de los pedidos
		long totalPedidos = pedidoDao.countPedidoById(id);

		// Aquí puedes usar el conteo para realizar otras tareas, por ejemplo, devolverlo en una respuesta personalizada
		return pedidos;
	}

	// Método para obtener el total de pedidos (útil si lo necesitas)
	public long getTotalPedidos(Long id) {
		return pedidoDao.countPedidoById(id);
	}
	//----------


/*
	@Override
	public Page<Pedido> findAllByCliente(Long idcliente, Pageable pageable) {
		return  pedidoDao.findPedidoById(idcliente, pageable);
	}*/

	//obetenemos los pedidos por cliente y estado para poder imprimir el reporte
	@Override
	public Iterable<Pedido> findAllByClienteAndEstado(String idcliente, String estado) {
		return pedidoDao.findByClienteOrEstadoReport(idcliente,estado);

	}

	@Override
	public Map<String, Integer> contarPedidosPorMes(Long clienteId) {
		return Map.of();
	}


	// Obtener los años con pedidos para un cliente

	@Override
	public List<Integer> obtenerAniosConPedidos(Long clienteId) {
		List<Pedido> pedidos = pedidoDao.findByClienteIds(clienteId);
		return pedidos.stream()
				.filter(p -> p.getFechaFinalizado() != null)
				.map(p -> p.getFechaFinalizado().toInstant().atZone(ZoneId.systemDefault()).getYear())
				.distinct()
				.sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
	}

	@Override
	public Map<String, Integer> contarPedidosPorMesYAnio(Long clienteId, int anio) {
		List<Pedido> pedidos = pedidoDao.findByClienteIds(clienteId);
		Map<String, Integer> resultado = new LinkedHashMap<>();

		String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
		for (String mes : meses) resultado.put(mes, 0);

		for (Pedido p : pedidos) {
			if (p.getFechaEntrega() != null) {
				LocalDate fecha = p.getFechaEntrega().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				if (fecha.getYear() == anio) {
					int mesIndex = fecha.getMonthValue() - 1; // 0-based
					resultado.put(meses[mesIndex], resultado.get(meses[mesIndex]) + 1);
				}
			}
		}
		log.info("Contador de pedidos por mes y año: " + resultado);
		return resultado;
	}


// Facturación anual para un cliente

	@Override
	public List<Integer> obtenerAniosConFacturacion(Long clienteId) {
		List<Pedido> pedidosCliente = pedidoDao.findByClienteIds(clienteId);
		return pedidosCliente.stream()
				.filter(p -> p.getFechaFinalizado() != null && p.getCobrado() != null)
				.map(p -> p.getFechaFinalizado().toInstant()
						.atZone(ZoneId.systemDefault())
						.toLocalDate().getYear())
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	// Facturación mensual para un año seleccionado
	@Override
	public Map<String, Double> totalFacturadoPorMesAnio(Long clienteId, int anio) {
		List<Pedido> pedidosCliente = pedidoDao.findByClienteIds(clienteId);
		Map<String, Double> totalPorMes = new LinkedHashMap<>();

		String[] mesesAbreviados = {"Ene", "Feb", "Mar", "Abr", "May", "Jun",
				"Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
		// Inicializar todos los meses en 0.0
		for (String mes : mesesAbreviados) {
			totalPorMes.put(mes, 0.0);
		}

		pedidosCliente.stream()
				.filter(p -> p.getFechaFinalizado() != null && p.getCobrado() != null)
				.map(p -> new AbstractMap.SimpleEntry<>(
						p.getFechaFinalizado().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
						p.getCobrado()))
				.filter(entry -> entry.getKey().getYear() == anio)
				.forEach(entry -> {
					int mesIndex = entry.getKey().getMonthValue() - 1;
					String mesAbreviado = mesesAbreviados[mesIndex];
					totalPorMes.put(mesAbreviado, totalPorMes.get(mesAbreviado) + entry.getValue());
				});

		return totalPorMes;
	}

	@Override
	@Transactional
	public Pedido findPedidoById(Long id) {
		Pedido pedido = pedidoDao.findByIdWithCliente(id);
		if (pedido != null && pedido.getCliente() != null) {
			pedido.getCliente().getId(); // fuerza la carga dentro de la sesión activa
			return pedido;
		}
		return pedido;
	}


	@Override
	public Map<String, Double> totalFacturadoPorMes(Long clienteId) {
		List<Pedido> pedidosCliente = pedidoDao.findByClienteIds(clienteId);
		Map<String, Double> totalPorMes = new LinkedHashMap<>();

		// Filtrar pedidos válidos
		List<Pedido> pedidosValidos = pedidosCliente.stream()
				.filter(p -> p.getFechaFinalizado() != null && p.getCobrado() != null)
				.collect(Collectors.toList());

		if (pedidosValidos.isEmpty()) {
			return totalPorMes; // Sin pedidos válidos, retornamos mapa vacío
		}

		// Obtener el rango de fechas (primer y último mes con pedidos)
		LocalDate minFecha = pedidosValidos.stream()
				.map(p -> p.getFechaFinalizado().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
				.min(LocalDate::compareTo)
				.orElse(LocalDate.now());

		LocalDate maxFecha = pedidosValidos.stream()
				.map(p -> p.getFechaFinalizado().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
				.max(LocalDate::compareTo)
				.orElse(LocalDate.now());

		// Inicializar el mapa con todos los meses del rango
		YearMonth start = YearMonth.from(minFecha);
		YearMonth end = YearMonth.from(maxFecha);

		YearMonth current = start;
		while (!current.isAfter(end)) {
			totalPorMes.put(current.toString(), 0.0); // formato YYYY-MM
			current = current.plusMonths(1);
		}

		// Sumar los importes a los meses correspondientes
		for (Pedido pedido : pedidosValidos) {
			LocalDate fecha = pedido.getFechaFinalizado().toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDate();

			String mes = YearMonth.from(fecha).toString(); // formato YYYY-MM
			totalPorMes.put(mes, totalPorMes.get(mes) + pedido.getCobrado());
		}

		log.info("Total facturado por mes (incluyendo meses sin facturación): {}");
		return totalPorMes;
	}




//	public Map<String, Integer> totalFacturadoPorMes(Long clienteId) {
//		// Obtenemos los pedidos del cliente
//		List<Pedido> pedidos = pedidoDao.findByClienteIds(clienteId);
//		// Mapa para almacenar el total facturado por mes
//		Map<String, Integer> resultado = new LinkedHashMap<>();
//
//		// Inicializar meses
//		String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
//		// Llenar el mapa con los meses y un valor inicial de 0
//		for (String mes : meses) resultado.put(mes, 0);
//
//		// Iterar sobre los pedidos
//		for (Pedido p : pedidos) {
//			if (pedido.getFechaEntrega() != null && pedido.getCobrado() != null) {
//				// Convertir Date a LocalDate para obtener el mes correctamente
//				Instant instant = p.getFechaEntrega().toInstant();
//				ZoneId zone = ZoneId.systemDefault();
//				LocalDate localDate = instant.atZone(zone).toLocalDate();
//				int mes = localDate.getMonthValue(); // 1 = enero
//				resultado.put(meses[mes - 1], resultado.get(meses[mes - 1]) + 1);
//			}
//		}
//		return resultado;
//	}




	@Override
	@Transactional(readOnly=true)
	public Page<Pedido> findAll(Pageable pageable) {
		return pedidoDao.findAll(pageable);
	}


	public JasperPrint generateJasperPrint(String cliente,String estado) throws IOException, JRException {

		org.springframework.core.io.Resource resourceFoto = resourceLoader.getResource("classpath:static/jasperReport/logo.png");
		InputStream logoEmpresa = resourceFoto.getInputStream();

		//obtener el listado de pedidos con parametros de estado y cliente
		Iterable<Pedido> pedido = findAllByClienteAndEstado(cliente,estado);

		// Obtener la ruta del archivo JRXML desde el directorio static
		org.springframework.core.io.Resource resources= resourceLoader.getResource("classpath:static/jasperReport/pedidos.jrxml");
		InputStream jrxmlFilePath = resources.getInputStream();

		// Obtener la plantilla del informe (.jrxml) y compilarla
		JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlFilePath);

		// Crear el DataSource del informe
		JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource((Collection<?>) pedido);

		// Mapa de parámetros para el informe
		Map<String, Object> parameters = new HashMap<>();

		// Llenar los parámetros del informe (si es necesario)
		parameters.put("ds", ds);
		parameters.put("logo", logoEmpresa);

		// Generar el informe
		return JasperFillManager.fillReport(jasperReport, parameters, ds);
	}



		/**
		 * Crea un pedido temporal vacío que se usará
		 * para asociar fotos antes de completar el pedido.
		 */
		public Pedido crearPedidoTemporal(Long idcliente) {
			log.info("Creando pedido temporal para cliente ID: {}"+idcliente);
			log.info("Creando pedido temporal para cliente ID: {}"+idcliente);

			Pedido pedido = new Pedido();

			// Obtener el cliente desde el servicio
			Cliente cliente = clienteService.findOne(idcliente);
			if (cliente == null) {
				throw new IllegalArgumentException("No se encontró cliente con ID: " + idcliente);
			}

			// Asignar cliente al pedido
			pedido.setCliente(cliente);

			// Generar un número temporal único
			String codigoTemporal = String.valueOf(System.currentTimeMillis());
			pedido.setNpedido(Long.valueOf(codigoTemporal));



			// Establecer campos base
			pedido.setEstado("TEMPORAL");
			pedido.setBorrador("true");
			pedido.setObservaciones("Pedido generado automáticamente al subir fotos.");


			// Guardar en base de datos
			Pedido pedidoGuardado = pedidoDao.save(pedido);

			log.info("Número temporal generado: {}"+pedidoGuardado.getNpedido());
			log.info("Pedido temporal creado con ID: {} y número: {}");
			return pedidoGuardado;
		}

}

