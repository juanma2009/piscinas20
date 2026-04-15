package com.bolsadeideas.springboot.app.models.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordActivationDTO {
    private String token;
    private String password;
}
