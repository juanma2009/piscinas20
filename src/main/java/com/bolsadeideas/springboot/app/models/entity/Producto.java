package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "productos")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class Producto implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id")
	private Empresa empresa;

	@ManyToOne
	@JoinColumn(name="NPROVEEDOR")
	private Proveedor nproveedor;

	@NotNull
	private String nombre;

	private String marca;

	private String foto;
	@NotNull
	private String codigo;

	@NotNull
	private Double precio;

	@NotNull
	private Double precioCompra;

	@NotNull
	private Double cantidad;


	private int descuento;

	private String vdetalle;

	@Temporal(TemporalType.DATE)
	@Column(name = "create_at")
	private Date createAt;

	/**
	 * ─── EXTENSIÓN JOYERÍA (todos opcionales, compatibles con datos existentes) ───
	 * Tipo de material precioso: ORO, PLATA, PLATINO, OTRO
	 */
	private String tipoMaterial;

	/** Pureza / ley: 18k, 14k, 9k, 925, 950, etc. */
	private String pureza;

	/** Unidad de medida del stock: GR (gramos), UDS (unidades), ML (mililitros) */
	@Column(columnDefinition = "VARCHAR(10) DEFAULT 'UDS'")
	private String unidadMedida = "UDS";

	/** Precio de referencia por gramo (para materiales nobles) */
	private Double precioPorGramo;

	@PrePersist
	public void prePersist() {
		createAt = new Date();
		if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
			Empresa e = new Empresa();
			e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
			this.empresa = e;
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}




	public String getMarca() {
		return marca;
	}

	public void setMarca(String marca) {
		this.marca = marca;
	}

	public int getDescuento() {
		return descuento;
	}

	public void setDescuento(int descuento) {
		this.descuento = descuento;
	}

	public Double getPrecio() {

		return precio;
	}

	public void setPrecio(Double precio) {
		this.precio = precio;
	}

	public Date getCreateAt() {
		return createAt;
	}

	public void setCreateAt(Date createAt) {
		this.createAt = createAt;
	}

	private static final long serialVersionUID = 1L;

	public Proveedor getNproveedor() {
		return nproveedor;
	}

	public void setNproveedor(Proveedor nproveedor) {
		this.nproveedor = nproveedor;
	}

	public String getFoto() {
		return foto;
	}

	public void setFoto(String foto) {
		this.foto = foto;
	}




	public String getVdetalle() {
		return vdetalle;
	}

	public void setVdetalle(String vdetalle) {
		this.vdetalle = vdetalle;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}



}
