package ec.edu.unibe.auditorio_backend.application.dto;

import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import jakarta.validation.constraints.*;

public record ActualizarUsuarioAdminDTO(
        @NotBlank(message = "La cédula es obligatoria")
        @Pattern(regexp = "^\\d{10}$", message = "La cédula debe contener 10 números")
        String username,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        @Pattern(regexp = "^[\\p{L}]+$", message = "El nombre debe contener solo letras")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
        @Pattern(regexp = "^[\\p{L}]+$", message = "El apellido debe contener solo letras")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correoInstitucional,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener 10 números")
        String telefono,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario role,

        boolean activo) {
}
