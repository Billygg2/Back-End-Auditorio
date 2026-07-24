package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservaNotificacionListener {

    private final EventoAuditorioService eventoService;
    private final NotificacionService notificacionService;

    public ReservaNotificacionListener(
            EventoAuditorioService eventoService,
            NotificacionService notificacionService) {
        this.eventoService = eventoService;
        this.notificacionService = notificacionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void alCrearReserva(ReservaCreadaEvent event) {
        EventoAuditorio evento = eventoService.obtenerEventoPorId(event.eventoId());
        notificacionService.notificarNuevaReservaAAdministradores(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void alCambiarEstado(ReservaEstadoCambiadoEvent event) {
        EventoAuditorio evento = eventoService.obtenerEventoPorId(event.eventoId());
        notificacionService.notificarCambioEstadoAlSolicitante(evento);
    }
}
