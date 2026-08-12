package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.DisponibilidadRecursosDTO;
import ec.edu.unibe.auditorio_backend.application.dto.DisponibilidadRecursosRequest;
import ec.edu.unibe.auditorio_backend.domain.service.DisponibilidadService;
import ec.edu.unibe.auditorio_backend.domain.service.RequerimientoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final RequerimientoService requerimientoService;

    public DisponibilidadController(
            DisponibilidadService disponibilidadService,
            RequerimientoService requerimientoService) {
        this.disponibilidadService = disponibilidadService;
        this.requerimientoService = requerimientoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin,
            @RequestParam(required = false) Long espacioId) {
        boolean disponible = espacioId == null
                ? disponibilidadService.verificarDisponibilidad(
                        fecha, LocalTime.parse(horaInicio), LocalTime.parse(horaFin))
                : disponibilidadService.verificarDisponibilidad(
                        espacioId, fecha, LocalTime.parse(horaInicio), LocalTime.parse(horaFin));
        return ResponseEntity.ok(disponible);
    }

    @PostMapping("/recursos")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DisponibilidadRecursosDTO> verificarRecursos(
            @Valid @RequestBody DisponibilidadRecursosRequest request) {
        return ResponseEntity.ok(requerimientoService.consultarDesdeSolicitud(
                request.requerimientos(),
                request.fecha(),
                request.horaInicio(),
                request.horaFin(),
                request.eventoId()));
    }
}
