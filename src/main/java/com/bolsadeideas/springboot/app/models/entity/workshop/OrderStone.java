package com.bolsadeideas.springboot.app.models.entity.workshop;

import com.bolsadeideas.springboot.app.models.entity.Pedido;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "order_stone")
@Data
public class OrderStone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private StoneFamily stoneFamily;

    private String stoneType;

    private String origin; // CUSTOMER/WORKSHOP

    @Enumerated(EnumType.STRING)
    private StoneShape shape;

    private String sizeMm;

    private BigDecimal carats;

    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String treatments;

    @ElementCollection(targetClass = StoneRiskFlag.class)
    @CollectionTable(name = "order_stone_risks", joinColumns = @JoinColumn(name = "order_stone_id"))
    @Enumerated(EnumType.STRING)
    private Set<StoneRiskFlag> riskFlags;

    private Boolean hasCertificate;

    private String certificateRef;
}
