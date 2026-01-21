package ec.edu.unibe.auditorio_backend.application.dto;

import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;

public class AprobacionEventoDTO {
    private EstadoEvento estado;
    private String motivoRechazo;

    public EstadoEvento getEstado() {
        return estado;
    }

    public void setEstado(EstadoEvento estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}