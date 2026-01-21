package ec.edu.unibe.auditorio_backend.domain.service;

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
import java.util.stream.Collectors;

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
        // Buscar usuario solicitante
        Usuario usuario = usuarioRepository.findByUsername(usernameSolicitante)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usernameSolicitante));
        
        // Asignar usuario solicitante al evento
        evento.setUsuarioSolicitante(usuario);
        
        // Siempre en estado PENDIENTE al crear
        evento.setEstado(EstadoEvento.PENDIENTE);
        
        // Validar disponibilidad
        if (!verificarDisponibilidad(evento.getFechaEvento(), 
                                     evento.getHoraInicio(), 
                                     evento.getHoraFin())) {
            throw new RuntimeException("Ya existe un evento APROBADO en ese horario");
        }

        // Guardar responsable (si es nuevo)
        if (evento.getResponsable() != null && evento.getResponsable().getId() == null) {
            Responsable responsable = responsableRepository.save(evento.getResponsable());
            evento.setResponsable(responsable);
        }

        // Enlazar requerimientos
        if (evento.getRequerimientos() != null) {
            for (Requerimiento r : evento.getRequerimientos()) {
                r.setEvento(evento);
            }
        }

        return eventoRepository.save(evento);
    }

    // Método para listar eventos del usuario que los solicitó
    public List<EventoAuditorio> listarEventosPorUsuario(String username) {
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));
        
        // Buscar eventos donde este usuario sea el solicitante
        return eventoRepository.findByUsuarioSolicitanteId(usuario.getId());
    }

    // El resto de métodos permanecen iguales (solo se actualizan los que usan usuario)
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
        
        // Verificar permisos: solo el solicitante o admin puede actualizar
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esSolicitante = eventoExistente.getUsuarioSolicitante() != null && 
                               eventoExistente.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRole().equals("ADMIN");
        
        if (!esSolicitante && !esAdmin) {
            throw new RuntimeException("No tiene permiso para actualizar este evento");
        }
        
        // Actualizar campos básicos
        eventoExistente.setNombreEvento(eventoActualizado.getNombreEvento());
        eventoExistente.setDescripcion(eventoActualizado.getDescripcion());
        eventoExistente.setFechaEvento(eventoActualizado.getFechaEvento());
        eventoExistente.setHoraInicio(eventoActualizado.getHoraInicio());
        eventoExistente.setHoraFin(eventoActualizado.getHoraFin());
        eventoExistente.setNumeroAsistentes(eventoActualizado.getNumeroAsistentes());
        eventoExistente.setPublicoExterno(eventoActualizado.isPublicoExterno());
        eventoExistente.setRequiereRegistroPrevio(eventoActualizado.isRequiereRegistroPrevio());
        eventoExistente.setTipoDisposicion(eventoActualizado.getTipoDisposicion());
        
        // Solo admin puede cambiar responsable
        if (esAdmin && eventoActualizado.getResponsable() != null) {
            Responsable responsableExistente = eventoExistente.getResponsable();
            Responsable responsableNuevo = eventoActualizado.getResponsable();
            
            responsableExistente.setNombre(responsableNuevo.getNombre());
            responsableExistente.setCorreo(responsableNuevo.getCorreo());
            responsableExistente.setTelefono(responsableNuevo.getTelefono());
            
            responsableRepository.save(responsableExistente);
        }
        
        // Actualizar requerimientos
        if (eventoActualizado.getRequerimientos() != null) {
            eventoExistente.getRequerimientos().clear();
            
            for (Requerimiento nuevoRequerimiento : eventoActualizado.getRequerimientos()) {
                nuevoRequerimiento.setEvento(eventoExistente);
                eventoExistente.getRequerimientos().add(nuevoRequerimiento);
            }
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
            if (!verificarDisponibilidad(evento.getFechaEvento(), 
                                         evento.getHoraInicio(), 
                                         evento.getHoraFin())) {
                throw new RuntimeException("No se puede aprobar: Ya existe un evento APROBADO en ese horario");
            }
        }
        
        evento.setEstado(aprobacionDTO.getEstado());
        
        if (aprobacionDTO.getEstado() == EstadoEvento.RECHAZADO) {
            evento.setMotivoRechazo(aprobacionDTO.getMotivoRechazo());
        } else {
            evento.setMotivoRechazo(null);
        }
        
        return eventoRepository.save(evento);
    }

    @Transactional
    public void eliminarEvento(Long id, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        
        // Verificar permisos
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esSolicitante = evento.getUsuarioSolicitante() != null && 
                               evento.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRole().equals("ADMIN");
        
        if (!esSolicitante && !esAdmin) {
            throw new RuntimeException("No tiene permiso para eliminar este evento");
        }
        
        // Solo se puede eliminar si está PENDIENTE o RECHAZADO
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
        return verificarDisponibilidad(fecha, 
                                      LocalTime.parse(horaInicio), 
                                      LocalTime.parse(horaFin));
    }

    public List<EventoAuditorio> obtenerEventosProximos(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        
        List<EventoAuditorio> eventosAprobados = eventoRepository.findByEstado(EstadoEvento.APROBADO);
        
        return eventosAprobados.stream()
                .filter(e -> !e.getFechaEvento().isBefore(hoy))
                .filter(e -> !e.getFechaEvento().isAfter(limite))
                .sorted((e1, e2) -> e1.getFechaEvento().compareTo(e2.getFechaEvento()))
                .collect(Collectors.toList());
    }

    @Transactional
    public EventoAuditorio cancelarEvento(Long id, String motivo, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        
        // Verificar permisos
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esSolicitante = evento.getUsuarioSolicitante() != null && 
                               evento.getUsuarioSolicitante().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRole().equals("ADMIN");
        
        if (!esSolicitante && !esAdmin) {
            throw new RuntimeException("No tiene permiso para cancelar este evento");
        }
        
        if (evento.getEstado() != EstadoEvento.PENDIENTE && 
            evento.getEstado() != EstadoEvento.APROBADO) {
            throw new RuntimeException("Solo se pueden cancelar eventos PENDIENTES o APROBADOS");
        }
        
        evento.setEstado(EstadoEvento.CANCELADO);
        if (motivo != null && !motivo.trim().isEmpty()) {
            evento.setMotivoRechazo(motivo);
        }
        
        return eventoRepository.save(evento);
    }
}