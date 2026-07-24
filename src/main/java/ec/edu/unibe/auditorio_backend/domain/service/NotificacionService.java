package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.application.dto.NotificacionDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Notificacion;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import ec.edu.unibe.auditorio_backend.domain.enums.TipoNotificacion;
import ec.edu.unibe.auditorio_backend.domain.repository.NotificacionRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void notificarNuevaReservaAAdministradores(EventoAuditorio evento) {
        List<Usuario> administradores = usuarioRepository.findByRoleAndActivoTrue(RolUsuario.ADMIN);
        List<Notificacion> notificaciones = new ArrayList<>();

        for (Usuario administrador : administradores) {
            notificaciones.add(crear(
                    administrador,
                    evento,
                    TipoNotificacion.NUEVA_RESERVA,
                    "Nueva reserva pendiente",
                    evento.getUsuarioSolicitante().getNombre() + " "
                            + evento.getUsuarioSolicitante().getApellido()
                            + " solicitó el auditorio para “" + evento.getNombreEvento() + "”."));
        }

        notificacionRepository.saveAll(notificaciones);
    }

    @Transactional
    public void notificarCambioEstadoAlSolicitante(EventoAuditorio evento) {
        Usuario solicitante = evento.getUsuarioSolicitante();
        if (solicitante == null) {
            return;
        }

        TipoNotificacion tipo = tipoPorEstado(evento.getEstado());
        String estado = textoEstado(evento.getEstado());
        String mensaje = "Tu reserva “" + evento.getNombreEvento() + "” fue " + estado + ".";

        if (evento.getMotivoRechazo() != null && !evento.getMotivoRechazo().isBlank()) {
            mensaje += " Motivo: " + evento.getMotivoRechazo();
        }

        notificacionRepository.save(crear(
                solicitante,
                evento,
                tipo,
                "Reserva " + estado,
                mensaje));
    }

    @Transactional
    public List<NotificacionDTO> listarDelUsuario(String username) {
        Usuario usuario = buscarUsuario(username);
        return notificacionRepository.findByDestinatarioIdOrderByCreadaEnDesc(usuario.getId())
                .stream()
                .map(this::aDTO)
                .toList();
    }

    public long contarNoLeidas(String username) {
        Usuario usuario = buscarUsuario(username);
        return notificacionRepository.countByDestinatarioIdAndLeidaFalse(usuario.getId());
    }

    @Transactional
    public NotificacionDTO marcarComoLeida(Long id, String username) {
        Usuario usuario = buscarUsuario(username);
        Notificacion notificacion = notificacionRepository.findByIdAndDestinatarioId(id, usuario.getId())
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        notificacion.setLeida(true);
        return aDTO(notificacionRepository.save(notificacion));
    }

    @Transactional
    public void marcarTodasComoLeidas(String username) {
        Usuario usuario = buscarUsuario(username);
        List<Notificacion> pendientes =
                notificacionRepository.findByDestinatarioIdAndLeidaFalse(usuario.getId());
        pendientes.forEach(notificacion -> notificacion.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    private Notificacion crear(
            Usuario destinatario,
            EventoAuditorio evento,
            TipoNotificacion tipo,
            String titulo,
            String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setEvento(evento);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        return notificacion;
    }

    private TipoNotificacion tipoPorEstado(EstadoEvento estado) {
        return switch (estado) {
            case APROBADO -> TipoNotificacion.RESERVA_APROBADA;
            case RECHAZADO -> TipoNotificacion.RESERVA_RECHAZADA;
            case CANCELADO -> TipoNotificacion.RESERVA_CANCELADA;
            case COMPLETADO -> TipoNotificacion.RESERVA_COMPLETADA;
            default -> throw new IllegalArgumentException("Estado no notificable: " + estado);
        };
    }

    private String textoEstado(EstadoEvento estado) {
        return switch (estado) {
            case APROBADO -> "aprobada";
            case RECHAZADO -> "rechazada";
            case CANCELADO -> "cancelada";
            case COMPLETADO -> "completada";
            default -> estado.name().toLowerCase();
        };
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
    }

    private NotificacionDTO aDTO(Notificacion notificacion) {
        return new NotificacionDTO(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getCreadaEn(),
                notificacion.getEvento() != null ? notificacion.getEvento().getId() : null);
    }
}
