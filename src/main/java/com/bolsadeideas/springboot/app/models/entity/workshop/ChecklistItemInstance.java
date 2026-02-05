package com.bolsadeideas.springboot.app.models.entity.workshop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "checklist_item_instance")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "instance", "templateItem"})
public class ChecklistItemInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id")
    private ChecklistInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_item_id")
    private ChecklistTemplateItem templateItem;

    private String itemCode;

    private String label;

    @Enumerated(EnumType.STRING)
    private InputType inputType;

    private boolean required;

    @Column(columnDefinition = "TEXT")
    private String options;

    private String conditionExpr;

    private Integer sortOrder;

    private Boolean checked;

    @Column(columnDefinition = "TEXT")
    private String valueText;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    private String completedBy;

    private Boolean readOnly = false;

    public Map<Object, Object> getValue() {
        Map<Object, Object> valueMap = new HashMap<>();

        if (checked != null) {
            valueMap.put("checked", checked);
        }
        if (valueText != null && !valueText.isEmpty()) {
            valueMap.put("valueText", valueText);
        }
        if (notes != null && !notes.isEmpty()) {
            valueMap.put("notes", notes);
        }
        if (completedAt != null) {
            valueMap.put("completedAt", completedAt);
        }
        if (completedBy != null && !completedBy.isEmpty()) {
            valueMap.put("completedBy", completedBy);
        }

        return valueMap;
    }
}
