package ec.edu.unibe.auditorio_backend.application.dto;

public record RecursoDisponibilidadDTO(
        Long tipoId,
        String nombre,
        int cantidadTotal,
        long cantidadOcupada,
        long cantidadDisponible,
        int cantidadSolicitada,
        boolean disponible) {
}
