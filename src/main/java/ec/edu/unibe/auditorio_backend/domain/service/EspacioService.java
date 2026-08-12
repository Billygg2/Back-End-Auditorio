package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import ec.edu.unibe.auditorio_backend.domain.repository.EspacioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EspacioService {

    public static final String CODIGO_AUDITORIO_PRINCIPAL = "AUD_B6_P1";

    private final EspacioRepository repository;

    public EspacioService(EspacioRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Espacio> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Espacio> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Espacio obtenerActivo(Long id) {
        Espacio espacio = obtener(id);
        if (!espacio.isActivo()) {
            throw new IllegalArgumentException("El espacio seleccionado no está disponible");
        }
        return espacio;
    }

    @Transactional(readOnly = true)
    public Espacio obtenerPredeterminado() {
        return repository.findByCodigo(CODIGO_AUDITORIO_PRINCIPAL)
                .orElseThrow(() -> new NoSuchElementException("No se configuró el auditorio principal"));
    }

    private Espacio obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio no encontrado: " + id));
    }
}
