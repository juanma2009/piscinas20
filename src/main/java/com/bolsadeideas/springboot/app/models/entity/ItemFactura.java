package com.bolsadeideas.springboot.app.models.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "facturas_items")
public class ItemFactura implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer cantidad;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getCantidad() {
		return cantidad;
	}

	public void setCantidad(Integer cantidad) {
		this.cantidad = cantidad;
	}

	public Double calcularImporte() {
		
		
		
		Double descuento= producto.getPrecio().doubleValue() * producto.getDescuento()/100;
		 Double precios = (producto.getPrecio().doubleValue()-descuento.doubleValue());
		
		 System.out.print("descuento  "+descuento.toString());
		 System.out.print("preciocondescuento  "+descuento.toString());
		 System.out.print("total  "+	cantidad.doubleValue() * precios.doubleValue());

		return cantidad.doubleValue() * precios.doubleValue();
		
		
	}

	public Producto getProducto() {
		return producto;
	}

	public void setProducto(Producto producto) {
		this.producto = producto;
	}
	
	private static final long serialVersionUID = 1L;

}