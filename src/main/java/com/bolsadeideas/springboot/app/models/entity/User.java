package com.bolsadeideas.springboot.app.models.entity;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "empresa_id = :tenantId")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true)
    private String username;

    @Column(length = 60)
    private String password;

    private boolean active = true; // Por defecto, el usuario está activo

    private String nombre;

    private String apellido;

    @Column
    private String email;

    private String puesto;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, columnDefinition = "varchar(30) default 'ACTIVO'")
    private UserStatus status = UserStatus.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /**
     * Relación muchos a muchos con la tabla roles
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();


    private Integer intentos;

    @Column(name = "secret")
    private String secret;  // Para doble factor

    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled;

    @PrePersist
    public void prePersist() {
        this.active = true;
        this.intentos = 0;
        if (this.empresa == null && com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant() != null) {
            Empresa e = new Empresa();
            e.setId(com.bolsadeideas.springboot.app.util.TenantContext.getCurrentTenant());
            this.empresa = e;
        }
    }

    /**
     * Método para codificar la contraseña
     *
     * @return
     */
    public List<GrantedAuthority> getGrantedAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getNombre())).collect(Collectors.toList());
    }


    // Métodos adicionales para manejo de roles
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }
}
