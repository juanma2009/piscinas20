package com.bolsadeideas.springboot.app.models.service;

import com.bolsadeideas.springboot.app.models.entity.configuracion.Servicio;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguracionService {

    // Aquí puedes agregar métodos para manejar la lógica de negocio relacionada con la configuración
    // Por ejemplo, cargar listas de servicios, metales, subgrupos y estados desde la base de datos
    // y devolverlas a los controladores para ser utilizadas en las vistas.

    // Ejemplo de método:
     public List<Servicio> obtenerServicios() {
    return servicioRepository.findAll();
    }
}
