package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.dto.*;
import ec.edu.unibe.auditorio_backend.domain.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listar(
            @RequestParam(required = false) String buscar) {
        return ResponseEntity.ok(usuarioService.listar(buscar));
    }

    @GetMapping("/paginado")
    public ResponseEntity<PaginaDTO<UsuarioDTO>> listarPaginado(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(required = false) String buscar) {
        return ResponseEntity.ok(PaginaDTO.desde(
                usuarioService.listarPaginado(buscar, pagina, tamanio)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioAdminDTO datos,
            Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.actualizarComoAdmin(id, datos, authentication.getName()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo,
            Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.cambiarEstado(id, activo, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UsuarioDTO> desactivar(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                usuarioService.cambiarEstado(id, false, authentication.getName()));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> restablecerPassword(
            @PathVariable Long id,
            @Valid @RequestBody RestablecerPasswordDTO datos) {
        usuarioService.restablecerPassword(id, datos);
        return ResponseEntity.noContent().build();
    }
}
