package ec.edu.unibe.auditorio_backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RequerimientoSolicitadoDTO(
        @NotNull Long tipoId,
        @Min(1) int cantidad) {
}
