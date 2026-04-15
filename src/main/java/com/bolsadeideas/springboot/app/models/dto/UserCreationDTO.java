package com.bolsadeideas.springboot.app.models.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserCreationDTO {
    private String username;
    private String email;
    private String nombre;
    private String apellido;
    private String puesto;
    private List<Long> roles;
    private boolean twoFactorEnabled;
}
