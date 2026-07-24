package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.*;
import ec.edu.unibe.auditorio_backend.domain.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/mi-cuenta")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MiCuentaController {

    private final UsuarioService usuarioService;

    public MiCuentaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<UsuarioDTO> obtener(Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.obtenerMiCuenta(authentication.getName()));
    }

    @PutMapping("/telefono")
    public ResponseEntity<UsuarioDTO> actualizarTelefono(
            @Valid @RequestBody ActualizarTelefonoDTO datos,
            Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.actualizarMiTelefono(authentication.getName(), datos));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambiarPasswordDTO datos,
            Authentication authentication) {
        usuarioService.cambiarMiPassword(authentication.getName(), datos);
        return ResponseEntity.noContent().build();
    }
}
