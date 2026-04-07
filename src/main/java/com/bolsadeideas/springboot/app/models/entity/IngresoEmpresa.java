package com.bolsadeideas.springboot.app.models.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ingresos_empresa")
@Getter
@Setter
public class IngresoEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private Double importe;
    private String concepto; // Ej. Cuota Mensual Abril
    
    @Temporal(TemporalType.DATE)
    private Date fecha;

    @PrePersist
    public void prePersist() {
        if (this.fecha == null) this.fecha = new Date();
    }
}
