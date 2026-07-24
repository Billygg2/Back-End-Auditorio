package ec.edu.unibe.auditorio_backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerPasswordDTO(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,64}$",
            message = "La contraseña debe tener entre 8 y 64 caracteres e incluir mayúscula, minúscula, número y símbolo"
        )
        String nuevaPassword) {
}
