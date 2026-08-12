package ec.edu.unibe.auditorio_backend.application.dto;

import java.util.List;

public record DisponibilidadRecursosDTO(
        boolean disponible,
        List<RecursoDisponibilidadDTO> recursos) {
}
