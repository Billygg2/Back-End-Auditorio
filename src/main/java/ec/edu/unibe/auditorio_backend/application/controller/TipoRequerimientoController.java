package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.domain.entity.TipoRequerimientoEntity;
import ec.edu.unibe.auditorio_backend.domain.service.TipoRequerimientoService;
import ec.edu.unibe.auditorio_backend.application.dto.PaginaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tipos-requerimiento")
public class TipoRequerimientoController {

    private final TipoRequerimientoService service;

    public TipoRequerimientoController(TipoRequerimientoService service) {
        this.service = service;
    }

    // Público: al crear reservas el usuario necesita ver los tipos activos
    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TipoRequerimientoEntity>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    // Solo admin: ver todos (activos e inactivos)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TipoRequerimientoEntity>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaginaDTO<TipoRequerimientoEntity>> listarPaginado(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ResponseEntity.ok(PaginaDTO.desde(
                service.listarPaginado(pagina, tamanio)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TipoRequerimientoEntity> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crear(@Valid @RequestBody TipoRequerimientoEntity tipo) {
        try {
            return new ResponseEntity<>(service.crear(tipo), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TipoRequerimientoEntity tipo) {
        try {
            return ResponseEntity.ok(service.actualizar(id, tipo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
