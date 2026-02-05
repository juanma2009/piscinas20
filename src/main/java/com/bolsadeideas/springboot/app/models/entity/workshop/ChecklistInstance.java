package com.bolsadeideas.springboot.app.models.entity.workshop;

import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "checklist_instance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "template_id"}))
@Data
public class ChecklistInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private ChecklistTemplate template;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ChecklistItemInstance> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
