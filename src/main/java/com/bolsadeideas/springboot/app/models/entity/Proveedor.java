package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name="PROVEEDOR")
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class Proveedor implements Serializable{


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IDENTIFICADOR")
    @SequenceGenerator(sequenceName = "proveedor_seq", allocationSize = 1, name = "IDENTIFICADOR")
	private Long nproveedor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id")
	private Empresa empresa;
	
	private String 	vnombre;
	private String 	vdireccion;
	private String 	vlocalidad;
	private String 	vprovincia;
	private String 	vpais;
	private String 	ntelefono;
	private String vemail;
	private String cif;
	@Temporal(TemporalType.DATE)	
	private Date dfecha_alta;
	
	@Temporal(TemporalType.DATE)	
	private Date dfecha_baja;

	@PrePersist
	public void prePersit() {
		dfecha_alta =new Date();
		if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
			Empresa e = new Empresa();
			e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
			this.empresa = e;
		}
	}
	

	public Proveedor() {
	
		super();
	}

	public String getCif() {
		return cif;
	}


	public void setCif(String cif) {
		this.cif = cif;
	}


	public Long getNproveedor() {
		return nproveedor;
	}

	public void setNproveedor(Long nproveedor) {
		this.nproveedor = nproveedor;
	}

	public String getVnombre() {
		return vnombre;
	}

	public void setVnombre(String vnombre) {
		this.vnombre = vnombre;
	}

	public String getVdireccion() {
		return vdireccion;
	}

	public void setVdireccion(String vdireccion) {
		this.vdireccion = vdireccion;
	}

	public String getVlocalidad() {
		return vlocalidad;
	}

	public void setVlocalidad(String vlocalidad) {
		this.vlocalidad = vlocalidad;
	}

	public String getVprovincia() {
		return vprovincia;
	}

	public void setVprovincia(String vprovincia) {
		this.vprovincia = vprovincia;
	}

	public String getVpais() {
		return vpais;
	}

	public void setVpais(String vpais) {
		this.vpais = vpais;
	}

	public String getNtelefono() {
		return ntelefono;
	}

	public void setNtelefono(String ntelefono) {
		this.ntelefono = ntelefono;
	}

	public String getVemail() {
		return vemail;
	}

	public void setVemail(String vemail) {
		this.vemail = vemail;
	}

	public Date getDfecha_alta() {
		return dfecha_alta;
	}

	public void setDfecha_alta(Date dfecha_alta) {
		this.dfecha_alta = dfecha_alta;
	}

	public Date getDfecha_baja() {
		return dfecha_baja;
	}

	public void setDfecha_baja(Date dfecha_baja) {
		this.dfecha_baja = dfecha_baja;
	}

	
	
}
