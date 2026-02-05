package com.bolsadeideas.springboot.app.models.entity.workshop;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "checklist_template")
@Data
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;

    private boolean active = true;

    private String version;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistTemplateSection> sections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
