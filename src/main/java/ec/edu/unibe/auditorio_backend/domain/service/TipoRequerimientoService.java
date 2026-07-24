package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.TipoRequerimientoEntity;
import ec.edu.unibe.auditorio_backend.domain.repository.TipoRequerimientoRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class TipoRequerimientoService {

    private final TipoRequerimientoRepository repository;

    public TipoRequerimientoService(TipoRequerimientoRepository repository) {
        this.repository = repository;
    }

    public List<TipoRequerimientoEntity> listarActivos() {
        return repository.findByActivoTrue();
    }

    public List<TipoRequerimientoEntity> listarTodos() {
        return repository.findAll();
    }

    public Page<TipoRequerimientoEntity> listarPaginado(int pagina, int tamanio) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), 100);
        return repository.findAll(PageRequest.of(
                paginaSegura,
                tamanioSeguro,
                Sort.by(Sort.Order.asc("nombre"))));
    }

    public TipoRequerimientoEntity obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de requerimiento no encontrado: " + id));
    }

    @Transactional
    public TipoRequerimientoEntity crear(TipoRequerimientoEntity tipo) {
        String nombre = tipo.getNombre().trim().toUpperCase();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new RuntimeException("Ya existe un tipo de requerimiento con ese nombre");
        }
        tipo.setNombre(nombre);
        tipo.setActivo(true);
        return repository.save(tipo);
    }

    @Transactional
    public TipoRequerimientoEntity actualizar(Long id, TipoRequerimientoEntity datos) {
        TipoRequerimientoEntity existente = obtenerPorId(id);
        String nuevoNombre = datos.getNombre().trim().toUpperCase();

        // Verificar duplicado solo si el nombre cambió
        if (!existente.getNombre().equalsIgnoreCase(nuevoNombre) 
                && repository.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new RuntimeException("Ya existe un tipo de requerimiento con ese nombre");
        }

        existente.setNombre(nuevoNombre);
        existente.setDescripcion(datos.getDescripcion());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        TipoRequerimientoEntity tipo = obtenerPorId(id);
        // Si tiene requerimientos asociados, solo desactivar
        repository.delete(tipo);
    }
}
