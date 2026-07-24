package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.NotificacionDTO;
import ec.edu.unibe.auditorio_backend.domain.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(notificacionService.listarDelUsuario(authentication.getName()));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "cantidad",
                notificacionService.contarNoLeidas(authentication.getName())));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<NotificacionDTO> marcarComoLeida(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                notificacionService.marcarComoLeida(id, authentication.getName()));
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas(Authentication authentication) {
        notificacionService.marcarTodasComoLeidas(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
