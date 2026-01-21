package ec.edu.unibe.auditorio_backend.domain.enums;

public enum EstadoEvento {
    PENDIENTE,      // Recién creado, esperando aprobación
    APROBADO,       // Aprobado por administrador
    RECHAZADO,      // Rechazado por administrador
    CANCELADO,      // Cancelado por el solicitante
    COMPLETADO      // Evento ya realizado
}