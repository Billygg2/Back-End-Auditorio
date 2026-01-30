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
import ec.edu.unibe.auditorio_backend.domain.repository.RequerimientoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventoAuditorioService {

    private final EventoAuditorioRepository eventoRepository;
    private final ResponsableRepository responsableRepository;
    private final UsuarioRepository usuarioRepository;
    private final RequerimientoRepository requerimientoRepository;

    public EventoAuditorioService(
            EventoAuditorioRepository eventoRepository,
            ResponsableRepository responsableRepository,
            UsuarioRepository usuarioRepository,
            RequerimientoRepository requerimientoRepository) {
        this.eventoRepository = eventoRepository;
        this.responsableRepository = responsableRepository;
        this.usuarioRepository = usuarioRepository;
        this.requerimientoRepository = requerimientoRepository;
    }

    @Transactional
    public EventoAuditorio crearEvento(EventoAuditorio evento, String usernameSolicitante) {
        Usuario usuario = buscarUsuarioPorUsername(usernameSolicitante);
        
        evento.setUsuarioSolicitante(usuario);
        evento.setEstado(EstadoEvento.PENDIENTE);
        
        // Verificar disponibilidad considerando eventos APROBADOS y PENDIENTES
        if (!verificarDisponibilidad(evento.getFechaEvento(), evento.getHoraInicio(), evento.getHoraFin())) {
            throw new RuntimeException("Ya existe un evento (APROBADO o PENDIENTE) en ese horario. Por favor, seleccione otra fecha u horario.");
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

    public boolean verificarDisponibilidadParaActualizacion(Long eventoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
    // Buscar eventos APROBADOS con conflicto de horario, excluyendo el evento actual
    List<EventoAuditorio> eventosAprobados = eventoRepository.findByEstado(EstadoEvento.APROBADO);
    boolean conflictoAprobados = eventosAprobados.stream()
        .filter(e -> !e.getId().equals(eventoId)) // Excluir el evento actual
        .filter(e -> e.getFechaEvento().equals(fecha))
        .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));
    
    // Buscar eventos PENDIENTES con conflicto de horario, excluyendo el evento actual
    List<EventoAuditorio> eventosPendientes = eventoRepository.findByEstado(EstadoEvento.PENDIENTE);
    boolean conflictoPendientes = eventosPendientes.stream()
        .filter(e -> !e.getId().equals(eventoId)) // Excluir el evento actual
        .filter(e -> e.getFechaEvento().equals(fecha))
        .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));
    
    // Disponible solo si NO hay conflictos con aprobados NI pendientes
    return !conflictoAprobados && !conflictoPendientes;
    }

    @Transactional
    public EventoAuditorio actualizarEvento(Long id, EventoAuditorio eventoActualizado, String username) {
    EventoAuditorio eventoExistente = obtenerEventoPorId(id);
    Usuario usuario = buscarUsuarioPorUsername(username);
    
    verificarPermisos(eventoExistente, usuario);
    
    // Verificar disponibilidad (excluyendo el evento actual)
    if (!verificarDisponibilidadParaActualizacion(
            eventoExistente.getId(),
            eventoActualizado.getFechaEvento(), 
            eventoActualizado.getHoraInicio(), 
            eventoActualizado.getHoraFin())) {
        throw new RuntimeException("Ya existe un evento (APROBADO o PENDIENTE) en ese horario. Por favor, seleccione otra fecha u horario.");
    }
    
    // Actualizar campos básicos
    actualizarCamposBasicos(eventoExistente, eventoActualizado);
    
    // Siempre actualizar responsable si se envía (tanto ADMIN como USER pueden actualizarlo)
    if (eventoActualizado.getResponsable() != null) {
        actualizarResponsable(eventoExistente, eventoActualizado.getResponsable());
    }
    
    // Actualizar requerimientos
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
            // Al aprobar, verificar solo conflictos con otros eventos APROBADOS
            // (no verificar pendientes porque este evento está cambiando de pendiente a aprobado)
            boolean conflictoAprobados = eventoRepository.tieneConflictoHorario(
                evento.getFechaEvento(), 
                evento.getHoraInicio(), 
                evento.getHoraFin()
            );
            
            if (conflictoAprobados) {
                throw new RuntimeException("No se puede aprobar: Ya existe otro evento APROBADO en ese horario");
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

    /**
     * ACTUALIZADO: Verifica disponibilidad considerando eventos APROBADOS y PENDIENTES
     * No se puede crear un evento si hay conflicto de horario con eventos aprobados o pendientes
     */
    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        // Buscar eventos APROBADOS con conflicto de horario
        boolean conflictoAprobados = eventoRepository.tieneConflictoHorario(fecha, horaInicio, horaFin);
        
        // Buscar eventos PENDIENTES con conflicto de horario
        List<EventoAuditorio> eventosPendientes = eventoRepository.findByEstado(EstadoEvento.PENDIENTE);
        boolean conflictoPendientes = eventosPendientes.stream()
            .filter(e -> e.getFechaEvento().equals(fecha))
            .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));
        
        // Disponible solo si NO hay conflictos con aprobados NI pendientes
        return !conflictoAprobados && !conflictoPendientes;
    }
    
    /**
     * Verifica si dos rangos de horas se solapan
     */
    private boolean hayConflictoHorario(LocalTime inicio1, LocalTime fin1, LocalTime inicio2, LocalTime fin2) {
        // Conflicto si:
        // - inicio2 está entre inicio1 y fin1, O
        // - fin2 está entre inicio1 y fin1, O
        // - inicio2 es antes de inicio1 Y fin2 es después de fin1 (envuelve completamente)
        return (inicio2.isBefore(fin1) && fin2.isAfter(inicio1));
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

    /**
     * MÉTODO CORREGIDO: Maneja correctamente la actualización de requerimientos
     * eliminando los viejos y creando los nuevos sin duplicar
     */
    private void actualizarRequerimientos(EventoAuditorio evento, List<Requerimiento> requerimientosNuevos) {
        // 1. Eliminar todos los requerimientos existentes de la base de datos
        if (evento.getRequerimientos() != null && !evento.getRequerimientos().isEmpty()) {
            requerimientoRepository.deleteAll(evento.getRequerimientos());
            evento.getRequerimientos().clear();
        }
        
        // 2. Crear una nueva lista para los requerimientos
        List<Requerimiento> nuevaLista = new ArrayList<>();
        
        // 3. Agregar los nuevos requerimientos
        if (requerimientosNuevos != null) {
            for (Requerimiento reqNuevo : requerimientosNuevos) {
                // Crear un nuevo objeto Requerimiento sin ID
                Requerimiento req = new Requerimiento();
                req.setTipo(reqNuevo.getTipo());
                req.setCantidad(reqNuevo.getCantidad());
                req.setRequerido(reqNuevo.isRequerido());
                req.setEvento(evento);
                nuevaLista.add(req);
            }
        }
        
        // 4. Asignar la nueva lista al evento
        evento.setRequerimientos(nuevaLista);
    }
}