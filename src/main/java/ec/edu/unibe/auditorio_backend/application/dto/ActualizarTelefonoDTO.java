package ec.edu.unibe.auditorio_backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarTelefonoDTO(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe contener 10 números")
        String telefono) {
}
