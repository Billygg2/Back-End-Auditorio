package ec.edu.unibe.auditorio_backend.domain.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservaEstadoCorreoListener {

    private final EventoAuditorioService eventoService;
    private final CorreoReservaService correoReservaService;

    public ReservaEstadoCorreoListener(
            EventoAuditorioService eventoService,
            CorreoReservaService correoReservaService) {
        this.eventoService = eventoService;
        this.correoReservaService = correoReservaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(ReservaEstadoCambiadoEvent event) {
        var evento = eventoService.obtenerEventoPorId(event.eventoId());
        correoReservaService.enviarCambioEstado(evento);
    }
}
