package com.bolsadeideas.springboot.app.models.service.configuracion;

import com.bolsadeideas.springboot.app.models.dao.configuracion.EstadoDao;
import com.bolsadeideas.springboot.app.models.entity.configuracion.Estado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class EstadoServiceImpl implements EstadoRepository {

    @Autowired
    private EstadoDao repository;

    @Override
    public Optional<Estado> findById(Long id) {
        // Aquí puedes implementar la lógica para buscar el estado por ID en la base de datos
        // Por ejemplo, usando un repositorio JPA o cualquier otra tecnología de persistencia
        // Retorna el estado encontrado, o un Optional vacío si no se encuentra
        // Puedes usar el repositorio para buscar el estado por ID
            return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        // Aquí puedes implementar la lógica para eliminar el estado de la base de datos
        // Por ejemplo, usando un repositorio JPA o cualquier otra tecnología de persistencia
        repository.deleteById(id);
        // No es necesario retornar nada, ya que el método es void

    }

    @Override
    public Estado save(Estado Metal) {
        // Aquí puedes implementar la lógica para guardar el estado en la base de datos
        // Por ejemplo, usando un repositorio JPA o cualquier otra tecnología de persistencia
         repository.save(Metal);
        // Retorna el estado guardado
        return Metal;

    }

    @Override
    public List<Estado> findAll() {
        // Aquí puedes implementar la lógica para obtener todos los estados de la base de datos
        // Por ejemplo, usando un repositorio JPA o cualquier otra tecnología de persistencia
        // Retorna la lista de estados
         List<Estado> estados = (List<Estado>) repository.findAll();
         if (estados != null) {
             return estados;
         }
             return Collections.emptyList();

    }

    @Override
    public Optional<Estado> actualizar(Long id, Estado estado) {
        // Aquí puedes implementar la lógica para actualizar el estado en la base de datos
        // Por ejemplo, usando un repositorio JPA o cualquier otra tecnología de persistencia
        // Retorna el estado actualizado
        Optional<Estado> estadoExistente = repository.findById(id);
        if (estadoExistente.isPresent()) {
            Estado estadoActualizado = estadoExistente.get();
            estadoActualizado.setNombre(estado.getNombre());
            estadoActualizado.setDescription(estado.getDescription());
            // Actualiza otros campos según sea necesario
            repository.save(estadoActualizado);
            return Optional.of(estadoActualizado);
        }
        return Optional.empty();
    }
}

