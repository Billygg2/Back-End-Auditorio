package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.service.EventoAuditorioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;  
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;      
import ec.edu.unibe.auditorio_backend.domain.service.EventoAuditorioService; 

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
public class EventoAuditorioController {

    private final EventoAuditorioService eventoService;

    public EventoAuditorioController(EventoAuditorioService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarTodosEventos() {
        return ResponseEntity.ok(eventoService.listarEventos());
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

    @GetMapping("/mis-eventos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<EventoAuditorio>> listarMisEventos(Authentication authentication) {
        return ResponseEntity.ok(eventoService.listarEventosPorUsuario(authentication.getName()));
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

    @PutMapping("/{id}/aprobar-rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoAuditorio> aprobarRechazarEvento(
            @PathVariable Long id,
            @RequestBody AprobacionEventoDTO aprobacionDTO) {
        return ResponseEntity.ok(eventoService.aprobarRechazarEvento(id, aprobacionDTO));
    }

    @GetMapping("/aprobados")
    public ResponseEntity<List<EventoAuditorio>> listarEventosAprobados() {
        return ResponseEntity.ok(eventoService.listarEventosPorEstado(EstadoEvento.APROBADO));
    }

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> obtenerEventoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerEventoPorId(id));
    }

@PutMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public ResponseEntity<?> actualizarEvento(
        @PathVariable Long id,
        @RequestBody EventoAuditorio eventoActualizado,
        Authentication authentication) {
    try {
        // Asegurarse de que el ID del path coincida con el del objeto
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> eliminarEvento(
            @PathVariable Long id,
            Authentication authentication) {
        eventoService.eliminarEvento(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventoAuditorio>> listarEventosPorFecha(@PathVariable LocalDate fecha) {
        return ResponseEntity.ok(eventoService.listarEventosPorFecha(fecha));
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin) {
        boolean disponible = eventoService.verificarDisponibilidad(
            fecha, 
            LocalTime.parse(horaInicio), 
            LocalTime.parse(horaFin)
        );
        return ResponseEntity.ok(disponible);
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<EventoAuditorio> cancelarEvento(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        return ResponseEntity.ok(eventoService.cancelarEvento(id, motivo, authentication.getName()));
    }

    @GetMapping("/proximos")
    public ResponseEntity<List<EventoAuditorio>> obtenerEventosProximos(
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(eventoService.obtenerEventosProximos(dias));
    }
}