package com.bolsadeideas.springboot.app.models.entity.workshop;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "checklist_template_item")
@Data
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private ChecklistTemplateSection section;

    private String itemCode;

    private String label;

    @Enumerated(EnumType.STRING)
    private InputType inputType;

    private boolean required;

    @Column(columnDefinition = "TEXT")
    private String options; // Separated by |

    private String conditionExpr;

    private Integer sortOrder;
}
