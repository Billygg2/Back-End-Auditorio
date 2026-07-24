package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.Responsable;
import ec.edu.unibe.auditorio_backend.domain.repository.ResponsableRepository;
import org.springframework.stereotype.Service;

@Service
public class ResponsableService {

    private final ResponsableRepository responsableRepository;

    public ResponsableService(ResponsableRepository responsableRepository) {
        this.responsableRepository = responsableRepository;
    }

    public Responsable guardarNuevo(Responsable responsable) {
        return responsableRepository.save(responsable);
    }

    public void actualizarResponsable(Responsable existente, Responsable nuevo) {
        existente.setNombre(nuevo.getNombre());
        existente.setCorreo(nuevo.getCorreo());
        existente.setTelefono(nuevo.getTelefono());
        responsableRepository.save(existente);
    }
}