package ec.edu.unibe.auditorio_backend.application.dto;

import ec.edu.unibe.auditorio_backend.domain.enums.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionDTO(
        Long id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime creadaEn,
        Long eventoId) {
}
