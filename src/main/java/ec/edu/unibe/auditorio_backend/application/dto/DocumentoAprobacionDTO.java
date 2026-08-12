package ec.edu.unibe.auditorio_backend.application.dto;

public record DocumentoAprobacionDTO(
        String nombre,
        String tipoContenido,
        long tamanio,
        byte[] contenido) {
}
