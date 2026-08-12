package ec.edu.unibe.auditorio_backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadRecursosRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        Long eventoId,
        @Valid List<RequerimientoSolicitadoDTO> requerimientos) {
}
