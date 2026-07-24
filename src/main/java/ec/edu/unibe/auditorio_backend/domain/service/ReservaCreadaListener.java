package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class ReservaCreadaListener {

    private final EventoAuditorioService eventoService;
    private final UsuarioRepository usuarioRepository;
    private final CorreoReservaService correoReservaService;

    public ReservaCreadaListener(
            EventoAuditorioService eventoService,
            UsuarioRepository usuarioRepository,
            CorreoReservaService correoReservaService) {
        this.eventoService = eventoService;
        this.usuarioRepository = usuarioRepository;
        this.correoReservaService = correoReservaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearReserva(ReservaCreadaEvent event) {
        EventoAuditorio evento = eventoService.obtenerEventoPorId(event.eventoId());
        List<Usuario> administradores = usuarioRepository.findByRoleAndActivoTrue(RolUsuario.ADMIN);
        administradores.forEach(admin -> correoReservaService.enviarNuevaReserva(evento, admin));
    }
}
