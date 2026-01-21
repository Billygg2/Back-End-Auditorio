package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.constants.RoleConstants;
import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento;
import ec.edu.unibe.auditorio_backend.domain.entity.Responsable;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.ResponsableRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class EventoAuditorioService {

    private final EventoAuditorioRepository eventoRepository;
    private final ResponsableRepository responsableRepository;
    private final UsuarioRepository usuarioRepository;

    public EventoAuditorioService(
            EventoAuditorioRepository eventoRepository,
            ResponsableRepository responsableRepository,
            UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.responsableRepository = responsableRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public EventoAuditorio crearEvento(EventoAuditorio evento, String usernameSolicitante) {
        Usuario usuario = buscarUsuarioPorUsername(usernameSolicitante);
        
        evento.setUsuarioSolicitante(usuario);
        evento.setEstado(EstadoEvento.PENDIENTE);
        
        if (!verificarDisponibilidad(evento.getFechaEvento(), evento.getHoraInicio(), evento.getHoraFin())) {
            throw new RuntimeException("Ya existe un evento APROBADO en ese horario");
        }

        if (evento.getResponsable() != null && evento.getResponsable().getId() == null) {
            Responsable responsable = responsableRepository.save(evento.getResponsable());
            evento.setResponsable(responsable);
        }

        if (evento.getRequerimientos() != null) {
            evento.getRequerimientos().forEach(r -> r.setEvento(evento));
        }

        return eventoRepository.save(evento);
    }

    public List<EventoAuditorio> listarEventosPorUsuario(String username) {
        Usuario usuario = buscarUsuarioPorUsername(username);
        return eventoRepository.findByUsuarioSolicitanteId(usuario.getId());
    }

    public List<EventoAuditorio> listarEventos() {
        return eventoRepository.findAll();
    }

    public EventoAuditorio obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
    }

    @Transactional
    public EventoAuditorio actualizarEvento(Long id, EventoAuditorio eventoActualizado, String username) {
        EventoAuditorio eventoExistente = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);
        
        verificarPermisos(eventoExistente, usuario);
        
        actualizarCamposBasicos(eventoExistente, eventoActualizado);
        
        if (esAdmin(usuario) && eventoActualizado.getResponsable() != null) {
            actualizarResponsable(eventoExistente, eventoActualizado.getResponsable());
        }
        
        if (eventoActualizado.getRequerimientos() != null) {
            actualizarRequerimientos(eventoExistente, eventoActualizado.getRequerimientos());
        }
        
        return eventoRepository.save(eventoExistente);
    }

    @Transactional
    public EventoAuditorio aprobarRechazarEvento(Long id, AprobacionEventoDTO aprobacionDTO) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar/rechazar eventos en estado PENDIENTE");
        }
        
        if (aprobacionDTO.getEstado() == EstadoEvento.APROBADO) {
            if (!verificarDisponibilidad(evento.getFechaEvento(), evento.getHoraInicio(), evento.getHoraFin())) {
                throw new RuntimeException("No se puede aprobar: Ya existe un evento APROBADO en ese horario");
            }
        }
        
        evento.setEstado(aprobacionDTO.getEstado());
        evento.setMotivoRechazo(
            aprobacionDTO.getEstado() == EstadoEvento.RECHAZADO ? aprobacionDTO.getMotivoRechazo() : null
        );
        
        return eventoRepository.save(evento);
    }

    @Transactional
    public void eliminarEvento(Long id, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);
        
        verificarPermisos(evento, usuario);
        
        if (evento.getEstado() == EstadoEvento.APROBADO) {
            throw new RuntimeException("No se puede eliminar un evento APROBADO");
        }
        
        eventoRepository.delete(evento);
    }

    public List<EventoAuditorio> listarEventosPorEstado(EstadoEvento estado) {
        return eventoRepository.findByEstado(estado);
    }

    public List<EventoAuditorio> listarEventosPorFecha(LocalDate fecha) {
        return eventoRepository.findByFechaEvento(fecha);
    }

    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return !eventoRepository.tieneConflictoHorario(fecha, horaInicio, horaFin);
    }

    public boolean verificarDisponibilidad(LocalDate fecha, String horaInicio, String horaFin) {
        return verificarDisponibilidad(fecha, LocalTime.parse(horaInicio), LocalTime.parse(horaFin));
    }

    public List<EventoAuditorio> obtenerEventosProximos(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        
        return eventoRepository.findByEstado(EstadoEvento.APROBADO).stream()
                .filter(e -> !e.getFechaEvento().isBefore(hoy) && !e.getFechaEvento().isAfter(limite))
                .sorted((e1, e2) -> e1.getFechaEvento().compareTo(e2.getFechaEvento()))
                .toList();
    }

    @Transactional
    public EventoAuditorio cancelarEvento(Long id, String motivo, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);
        
        verificarPermisos(evento, usuario);
        
        if (evento.getEstado() != EstadoEvento.PENDIENTE && evento.getEstado() != EstadoEvento.APROBADO) {
            throw new RuntimeException("Solo se pueden cancelar eventos PENDIENTES o APROBADOS");
        }
        
        evento.setEstado(EstadoEvento.CANCELADO);
        if (motivo != null && !motivo.trim().isEmpty()) {
            evento.setMotivoRechazo(motivo);
        }
        
        return eventoRepository.save(evento);
    }

    // ==================== MÉTODOS PRIVADOS ====================
    
    private Usuario buscarUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
    }

    private void verificarPermisos(EventoAuditorio evento, Usuario usuario) {
        boolean esSolicitante = evento.getUsuarioSolicitante() != null && 
                               evento.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean esAdmin = esAdmin(usuario);
        
        if (!esSolicitante && !esAdmin) {
            throw new RuntimeException("No tiene permiso para realizar esta acción en este evento");
        }
    }

    private boolean esAdmin(Usuario usuario) {
        return RoleConstants.ADMIN.equals(usuario.getRole());
    }

    private void actualizarCamposBasicos(EventoAuditorio existente, EventoAuditorio actualizado) {
        existente.setNombreEvento(actualizado.getNombreEvento());
        existente.setDescripcion(actualizado.getDescripcion());
        existente.setFechaEvento(actualizado.getFechaEvento());
        existente.setHoraInicio(actualizado.getHoraInicio());
        existente.setHoraFin(actualizado.getHoraFin());
        existente.setNumeroAsistentes(actualizado.getNumeroAsistentes());
        existente.setPublicoExterno(actualizado.isPublicoExterno());
        existente.setRequiereRegistroPrevio(actualizado.isRequiereRegistroPrevio());
        existente.setTipoDisposicion(actualizado.getTipoDisposicion());
    }

    private void actualizarResponsable(EventoAuditorio evento, Responsable responsableNuevo) {
        Responsable responsableExistente = evento.getResponsable();
        responsableExistente.setNombre(responsableNuevo.getNombre());
        responsableExistente.setCorreo(responsableNuevo.getCorreo());
        responsableExistente.setTelefono(responsableNuevo.getTelefono());
        responsableRepository.save(responsableExistente);
    }

    private void actualizarRequerimientos(EventoAuditorio evento, List<Requerimiento> requerimientosNuevos) {
        evento.getRequerimientos().clear();
        requerimientosNuevos.forEach(req -> {
            req.setEvento(evento);
            evento.getRequerimientos().add(req);
        });
    }
}