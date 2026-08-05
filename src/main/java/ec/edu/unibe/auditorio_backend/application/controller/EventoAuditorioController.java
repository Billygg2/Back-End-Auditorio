package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.application.dto.PaginaDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.service.EventoAuditorioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
public class EventoAuditorioController {

    private final EventoAuditorioService eventoService;

    public EventoAuditorioController(EventoAuditorioService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/completados")
    public ResponseEntity<List<EventoAuditorio>> listarCompletados() {
        return ResponseEntity.ok(eventoService.listarEventosCompletados());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarTodosEventos() {
        return ResponseEntity.ok(eventoService.listarEventos());
    }

    @GetMapping("/mis-eventos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<EventoAuditorio>> listarMisEventos(Authentication authentication) {
        return ResponseEntity.ok(eventoService.listarEventosPorUsuario(authentication.getName()));
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PaginaDTO<EventoAuditorio>> listarPaginado(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "5") int tamanio,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) EstadoEvento estado,
            Authentication authentication) {
        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(PaginaDTO.desde(eventoService.listarPaginado(
                authentication.getName(), administrador, buscar, estado, pagina, tamanio)));
    }

    @GetMapping("/aprobados")
    public ResponseEntity<List<EventoAuditorio>> listarEventosAprobados() {
        return ResponseEntity.ok(eventoService.listarEventosPorEstado(EstadoEvento.APROBADO));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosPendientes() {
        return ResponseEntity.ok(eventoService.listarEventosPorEstado(EstadoEvento.PENDIENTE));
    }

    @GetMapping("/rechazados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosRechazados() {
        return ResponseEntity.ok(eventoService.listarEventosPorEstado(EstadoEvento.RECHAZADO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> obtenerEventoPorId(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                eventoService.obtenerEventoPorIdAutorizado(id, authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> crearEvento(
            @RequestBody EventoAuditorio evento,
            Authentication authentication) {
        try {
            EventoAuditorio creado = eventoService.crearEvento(evento, authentication.getName());
            return new ResponseEntity<>(creado, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> actualizarEvento(
            @PathVariable Long id,
            @RequestBody EventoAuditorio eventoActualizado,
            Authentication authentication) {
        try {
            eventoActualizado.setId(id);
            EventoAuditorio evento = eventoService.actualizarEvento(id, eventoActualizado, authentication.getName());
            return ResponseEntity.ok(evento);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }

    @PutMapping("/{id}/aprobar-rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoAuditorio> aprobarRechazarEvento(
            @PathVariable Long id,
            @RequestBody AprobacionEventoDTO aprobacionDTO) {
        return ResponseEntity.ok(eventoService.aprobarRechazarEvento(id, aprobacionDTO));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<EventoAuditorio> cancelarEvento(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id, motivo, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> eliminarEvento(
            @PathVariable Long id,
            Authentication authentication) {
        eventoService.eliminarEvento(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
