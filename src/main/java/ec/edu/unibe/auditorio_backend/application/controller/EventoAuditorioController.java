package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.service.EventoAuditorioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class EventoAuditorioController {

    private final EventoAuditorioService eventoService;

    public EventoAuditorioController(EventoAuditorioService eventoService) {
        this.eventoService = eventoService;
    }

    // ========== ENDPOINT PARA ADMIN (VER TODOS) ==========
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarTodosEventos() {
        List<EventoAuditorio> eventos = eventoService.listarEventos();
        return ResponseEntity.ok(eventos);
    }

    // ========== ENDPOINTS PARA USUARIOS ==========
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> crearEvento(@RequestBody EventoAuditorio evento) {
        try {
            String username = obtenerUsernameAutenticado();
            EventoAuditorio creado = eventoService.crearEvento(evento, username);
            return new ResponseEntity<>(creado, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && 
                (e.getMessage().contains("Ya existe un evento APROBADO") ||
                 e.getMessage().contains("no está disponible"))) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("{\"error\": \"" + e.getMessage() + "\"}");
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno del servidor\"}");
        }
    }

    @GetMapping("/mis-eventos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<EventoAuditorio>> listarMisEventos() {
        String username = obtenerUsernameAutenticado();
        List<EventoAuditorio> eventos = eventoService.listarEventosPorUsuario(username);
        return ResponseEntity.ok(eventos);
    }

    // ========== ENDPOINTS PARA ADMINISTRADORES ==========
    
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosPendientes() {
        List<EventoAuditorio> eventos = eventoService.listarEventosPorEstado(EstadoEvento.PENDIENTE);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/rechazados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosRechazados() {
        List<EventoAuditorio> eventos = eventoService.listarEventosPorEstado(EstadoEvento.RECHAZADO);
        return ResponseEntity.ok(eventos);
    }

    @PutMapping("/{id}/aprobar-rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoAuditorio> aprobarRechazarEvento(
            @PathVariable Long id,
            @RequestBody AprobacionEventoDTO aprobacionDTO) {
        EventoAuditorio evento = eventoService.aprobarRechazarEvento(id, aprobacionDTO);
        return ResponseEntity.ok(evento);
    }

    // ========== ENDPOINTS PÚBLICOS O PARA TODOS ==========
    
    @GetMapping("/aprobados")
    public ResponseEntity<List<EventoAuditorio>> listarEventosAprobados() {
        List<EventoAuditorio> eventos = eventoService.listarEventosPorEstado(EstadoEvento.APROBADO);
        return ResponseEntity.ok(eventos);
    }

    // NUEVO ENDPOINT PARA CALENDARIO COMPLETO
    @GetMapping("/calendario-completo")
    public ResponseEntity<Map<String, List<EventoAuditorio>>> listarEventosCalendarioCompleto(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin) {
        
        List<EventoAuditorio> aprobados = eventoService.listarEventosPorEstado(EstadoEvento.APROBADO);
        List<EventoAuditorio> pendientes = eventoService.listarEventosPorEstado(EstadoEvento.PENDIENTE);
        
        if (fechaInicio != null && fechaFin != null) {
            aprobados = aprobados.stream()
                    .filter(e -> !e.getFechaEvento().isBefore(fechaInicio) && 
                                 !e.getFechaEvento().isAfter(fechaFin))
                    .collect(Collectors.toList());
            
            pendientes = pendientes.stream()
                    .filter(e -> !e.getFechaEvento().isBefore(fechaInicio) && 
                                 !e.getFechaEvento().isAfter(fechaFin))
                    .collect(Collectors.toList());
        }
        
        Map<String, List<EventoAuditorio>> respuesta = new HashMap<>();
        respuesta.put("aprobados", aprobados);
        respuesta.put("pendientes", pendientes);
        
        return ResponseEntity.ok(respuesta);
    }

    // ========== ENDPOINTS GENERALES CON PERMISOS ==========
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> obtenerEventoPorId(@PathVariable Long id) {
        EventoAuditorio evento = eventoService.obtenerEventoPorId(id);
        if (!tienePermisoParaVerEvento(evento)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(evento);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> actualizarEvento(
            @PathVariable Long id,
            @RequestBody EventoAuditorio eventoActualizado) {
        String username = obtenerUsernameAutenticado();
        EventoAuditorio evento = eventoService.actualizarEvento(id, eventoActualizado, username);
        return ResponseEntity.ok(evento);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> eliminarEvento(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();
        eventoService.eliminarEvento(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosPorFecha(
            @PathVariable LocalDate fecha) {
        List<EventoAuditorio> eventos = eventoService.listarEventosPorFecha(fecha);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin) {
        boolean disponible = eventoService.verificarDisponibilidad(fecha, horaInicio, horaFin);
        return ResponseEntity.ok(disponible);
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> cancelarEvento(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        String username = obtenerUsernameAutenticado();
        EventoAuditorio evento = eventoService.cancelarEvento(id, motivo, username);
        return ResponseEntity.ok(evento);
    }

    @GetMapping("/proximos")
    public ResponseEntity<List<EventoAuditorio>> obtenerEventosProximos(
            @RequestParam(defaultValue = "7") int dias) {
        List<EventoAuditorio> eventos = eventoService.obtenerEventosProximos(dias);
        return ResponseEntity.ok(eventos);
    }

    // ========== MÉTODOS AUXILIARES PRIVADOS ==========
    
    private String obtenerUsernameAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
    
    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
    
    private boolean tienePermisoParaVerEvento(EventoAuditorio evento) {
        if (esAdmin()) {
            return true;
        }
        String username = obtenerUsernameAutenticado();
        if (evento.getUsuarioSolicitante() != null) {
            return evento.getUsuarioSolicitante().getUsername().equals(username);
        }
        return false;
    }
}