package ec.edu.unibe.auditorio_backend.application.dto;

import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;

public record UsuarioDTO(
        Long id,
        String username,
        String nombre,
        String apellido,
        String nombreCompleto,
        String correoInstitucional,
        String telefono,
        RolUsuario role,
        boolean activo,
        boolean debeCambiarPassword) {
}
