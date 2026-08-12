package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import ec.edu.unibe.auditorio_backend.domain.service.EspacioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class EspacioController {

    private final EspacioService service;

    public EspacioController(EspacioService service) {
        this.service = service;
    }

    @GetMapping("/activos")
    public ResponseEntity<List<Espacio>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Espacio>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }
}
