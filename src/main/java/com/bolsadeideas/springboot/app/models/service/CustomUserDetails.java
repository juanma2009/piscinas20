package com.bolsadeideas.springboot.app.models.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {
    private Long empresaId;
    private String empresaNombre; // 👈 Añadido
    private String nombre;
    private String apellido;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long empresaId, String empresaNombre) {
        super(username, password, authorities);
        this.empresaId = empresaId;
        this.empresaNombre = empresaNombre;
    }

    public CustomUserDetails(String username, String password, boolean enabled, boolean accountNonExpired,
                             boolean credentialsNonExpired, boolean accountNonLocked,
                             Collection<? extends GrantedAuthority> authorities,
                             Long empresaId, String empresaNombre, String nombre, String apellido) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.empresaId = empresaId;
        this.empresaNombre = empresaNombre;
        this.nombre    = nombre;
        this.apellido  = apellido;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    /** Devuelve nombre + apellido, o el username si no hay nombre registrado */
    public String getNombreCompleto() {
        if (nombre != null && !nombre.isBlank()) {
            return apellido != null && !apellido.isBlank()
                    ? nombre + " " + apellido
                    : nombre;
        }
        return getUsername();
    }
}
