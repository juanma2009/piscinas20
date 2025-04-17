package com.bolsadeideas.springboot.app.models.service;


import com.bolsadeideas.springboot.app.models.dao.PedidoDao;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class PedidoServiceImpl implements PedidoService {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private PedidoDao pedidoDao;

	@Override
	@Transactional(readOnly=true)
	public Iterable<Pedido> findAll() {
		return  pedidoDao.findAll();
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


	public  Page<Pedido> buscarPedidos(String cliente,String tipoPedido, String estado, String grupo,
											  String subgrupo, Date fechaDesde, Date fechaHasta, Pageable pageable)
	{

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Pedido> cq = cb.createQuery(Pedido.class);
		Root<Pedido> pedido = cq.from(Pedido.class);

		List<Predicate> predicates = new ArrayList<>();

		if (cliente != null && !cliente.isEmpty()) {
			predicates.add(cb.like(cb.lower(pedido.get("cliente").get("nombre")), "%" + cliente.toLowerCase() + "%"));
		}
		if (tipoPedido != null && !tipoPedido.isEmpty()) {
			predicates.add(cb.equal(pedido.get("tipoPedido"),tipoPedido));
		}
		if (estado != null && !estado.isEmpty()) {
			predicates.add(cb.like(cb.lower(pedido.get("estado")), "%" + estado.toLowerCase() + "%"));
		}
		if (grupo != null && !grupo.isEmpty()) {
			predicates.add(cb.equal(pedido.get("grupo"), grupo));
		}
		if (subgrupo != null && !subgrupo.isEmpty()) {
			predicates.add(cb.equal(pedido.get("subgrupo"), subgrupo));
		}
		if (fechaDesde != null) {
			predicates.add(cb.greaterThanOrEqualTo(pedido.get("dfecha"), fechaDesde));
		}
		if (fechaHasta != null) {
			predicates.add(cb.lessThanOrEqualTo(pedido.get("dfecha"), fechaHasta));
		}

		cq.where(predicates.toArray(new Predicate[0]));

		TypedQuery<Pedido> query = entityManager.createQuery(cq);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		List<Pedido> pedidos = query.getResultList();

		// Contar total de registros para la paginación
		CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
		Root<Pedido> countRoot = countQuery.from(Pedido.class);
		countQuery.select(cb.count(countRoot)).where(predicates.toArray(new Predicate[0]));
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
		return (List<Pedido>) pedidoDao.findAll();
	}

	public Page<Pedido> findPedidoByIdClienteAndFinalizados(Long idCliente, Pageable pageable) {
		return  pedidoDao.findPedidosByIdClienteAndTerminado(idCliente, pageable);
	}

	@Override
	public Page<Pedido> findAllByCliente(Long idcliente, Pageable pageable) {
		return pedidoDao.findByClienteId(idcliente, pageable);
	}

	//obetenemos los pedidos por cliente y estado para poder imprimir el reporte
	@Override
	public Iterable<Pedido> findAllByClienteAndEstado(String idcliente, String estado) {
		return pedidoDao.findByClienteOrEstadoReport(idcliente,estado);

	}

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


}