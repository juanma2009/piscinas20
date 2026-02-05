package com.bolsadeideas.springboot.app.models.entity;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"archivosAdjuntos", "comentarios"})
@EqualsAndHashCode(exclude = {"archivosAdjuntos", "comentarios"})
@Table(name = "PEDIDO")
public class Pedido implements Serializable {
    private static final long serialVersionUID = 1456456L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IDENTIFICADOR")
    @SequenceGenerator(sequenceName = "pedido_seq", allocationSize = 1, name = "IDENTIFICADOR")
    private Long npedido;

    @ManyToOne(fetch = FetchType.EAGER)
    private Cliente cliente;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    private Date dfecha;

    private String observacion;

    //se añade el estado del pedido con un valor por defecto de PENDIENTE
    private String estado;

    private String tipoPedido;

    private Boolean facturado = false;//defecto es false

    private Boolean enviadoSms = false;//defecto es false

    private String grupo;

    private String subgrupo;

    private String pieza;

    private String tipo;

    private Double peso;

    private String ref;

    private String horas;

    private Double cobrado;

    private String empleado;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaFinalizado;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaEntrega;

    //fecha de envio de sms
    @Temporal(TemporalType.DATE)
    private Date fechaEnvioSms;

    //estado de envio del sms (enviado, no enviado)
    private String estadoEnvioSms;

    private String borrador;

    private String observaciones;

    //añadir una variable que guarde los archivos adjuntos en un arraylist

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArchivoAdjunto> archivosAdjuntos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        dfecha = new Date();
    }

    // Relación con Comentario
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios = new ArrayList<>();

    public String getClientes() {
      return  this.cliente != null ? this.cliente.getNombre():"---";


    }

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;  // Valor por defecto: true (todos los existentes quedan activos)
//----metodos /

    // Método útil para dar de baja
    public void darDeBaja() {
        this.activo = false;
    }

    // Método para reactivar (por si acaso)
    public void reactivar() {
        this.activo = true;
    }

    // Métodos para agregar y eliminar comentarios
    public void addComentario(Comentario comentario) {
        comentarios.add(comentario);
        comentario.setPedido(this);
    }

    public void removeComentario(Comentario comentario) {
        comentarios.remove(comentario);
        comentario.setPedido(null);
    }



}



