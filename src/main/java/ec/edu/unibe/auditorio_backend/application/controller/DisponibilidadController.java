package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.domain.service.DisponibilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin) {
        boolean disponible = disponibilidadService.verificarDisponibilidad(
                fecha,
                LocalTime.parse(horaInicio),
                LocalTime.parse(horaFin));
        return ResponseEntity.ok(disponible);
    }
}
