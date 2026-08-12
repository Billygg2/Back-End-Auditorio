package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.service.EventoAuditorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final EventoAuditorioService eventoService;

    public CalendarioController(EventoAuditorioService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/completo")
    public ResponseEntity<Map<String, List<EventoAuditorio>>> listarEventosCalendarioCompleto(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) Long espacioId) {

        List<EventoAuditorio> aprobados = espacioId == null
                ? eventoService.listarEventosPorEstado(EstadoEvento.APROBADO)
                : eventoService.listarEventosPorEstadoYEspacio(EstadoEvento.APROBADO, espacioId);

        List<EventoAuditorio> pendientes = espacioId == null
                ? eventoService.listarEventosPorEstado(EstadoEvento.PENDIENTE)
                : eventoService.listarEventosPorEstadoYEspacio(EstadoEvento.PENDIENTE, espacioId);

        List<EventoAuditorio> completados = espacioId == null
                ? eventoService.listarEventosPorEstado(EstadoEvento.COMPLETADO)
                : eventoService.listarEventosPorEstadoYEspacio(EstadoEvento.COMPLETADO, espacioId);

        if (fechaInicio != null && fechaFin != null) {

            aprobados = aprobados.stream()
                    .filter(e -> !e.getFechaEvento().isBefore(fechaInicio)
                            && !e.getFechaEvento().isAfter(fechaFin))
                    .toList();

            pendientes = pendientes.stream()
                    .filter(e -> !e.getFechaEvento().isBefore(fechaInicio)
                            && !e.getFechaEvento().isAfter(fechaFin))
                    .toList();

            completados = completados.stream()
                    .filter(e -> !e.getFechaEvento().isBefore(fechaInicio)
                            && !e.getFechaEvento().isAfter(fechaFin))
                    .toList();
        }

        Map<String, List<EventoAuditorio>> respuesta = new HashMap<>();

        respuesta.put("aprobados", aprobados);
        respuesta.put("pendientes", pendientes);
        respuesta.put("completados", completados);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/proximos")
    public ResponseEntity<List<EventoAuditorio>> obtenerEventosProximos(
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(eventoService.obtenerEventosProximos(dias));
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<EventoAuditorio>> listarEventosPorFecha(
            @PathVariable LocalDate fecha,
            @RequestParam(required = false) Long espacioId) {
        return ResponseEntity.ok(espacioId == null
                ? eventoService.listarEventosPorFecha(fecha)
                : eventoService.listarEventosPorFechaYEspacio(fecha, espacioId));
    }
}
