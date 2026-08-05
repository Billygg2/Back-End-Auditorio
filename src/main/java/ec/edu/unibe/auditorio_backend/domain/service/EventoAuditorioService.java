package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Responsable;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled; // ← NUEVO IMPORT
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class EventoAuditorioService {

    private final EventoAuditorioRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadService disponibilidadService;
    private final RequerimientoService requerimientoService;
    private final ResponsableService responsableService;
    private final ApplicationEventPublisher eventPublisher;

    public EventoAuditorioService(
            EventoAuditorioRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            DisponibilidadService disponibilidadService,
            RequerimientoService requerimientoService,
            ResponsableService responsableService,
            ApplicationEventPublisher eventPublisher) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadService = disponibilidadService;
        this.requerimientoService = requerimientoService;
        this.responsableService = responsableService;
        this.eventPublisher = eventPublisher;
    }

    // ==================== SCHEDULER AUTOMÁTICO ====================
    /**
     * Se ejecuta todos los días a la 1:00 AM.
     * Busca eventos APROBADOS cuya fecha ya pasó y los marca como COMPLETADO en BD.
     */
    @PostConstruct
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void marcarEventosCompletadosAutomaticamente() {
        LocalDate hoy = LocalDate.now();

        List<EventoAuditorio> eventosAprobados = eventoRepository.findByEstado(EstadoEvento.APROBADO);

        List<EventoAuditorio> aCompletar = eventosAprobados.stream()
                .filter(evento -> evento.getFechaEvento().isBefore(hoy))
                .toList();

        if (!aCompletar.isEmpty()) {
            aCompletar.forEach(evento -> evento.setEstado(EstadoEvento.COMPLETADO));
            eventoRepository.saveAll(aCompletar); // más eficiente que save() uno a uno
        }
    }

    // ==================== CRUD PRINCIPAL ====================

    @Transactional
    public EventoAuditorio crearEvento(EventoAuditorio evento, String usernameSolicitante) {
        Usuario usuario = buscarUsuarioPorUsername(usernameSolicitante);
        evento.setUsuarioSolicitante(usuario);
        evento.setEstado(EstadoEvento.PENDIENTE);

        // ← NUEVO: validar mínimo 2 semanas de anticipación
        if (evento.getFechaEvento().isBefore(LocalDate.now().plusDays(14))) {
            throw new RuntimeException(
                    "La fecha del evento debe ser con al menos 2 semanas de anticipación desde hoy.");
        }

        if (!disponibilidadService.verificarDisponibilidad(
                evento.getFechaEvento(), evento.getHoraInicio(), evento.getHoraFin())) {
            throw new RuntimeException(
                    "Ya existe un evento (APROBADO o PENDIENTE) en ese horario. Por favor, seleccione otra fecha u horario.");
        }

        if (evento.getResponsable() != null && evento.getResponsable().getId() == null) {
            Responsable responsable = responsableService.guardarNuevo(evento.getResponsable());
            evento.setResponsable(responsable);
        }

        requerimientoService.vincularRequerimientos(evento, evento.getRequerimientos());

        EventoAuditorio guardado = eventoRepository.save(evento);
        eventPublisher.publishEvent(new ReservaCreadaEvent(guardado.getId()));
        return guardado;
    }

    @Transactional
    public EventoAuditorio actualizarEvento(Long id, EventoAuditorio eventoActualizado, String username) {
        EventoAuditorio eventoExistente = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);

        verificarPermisos(eventoExistente, usuario);

        // ← NUEVO: validar mínimo 2 semanas también al actualizar
        if (eventoActualizado.getFechaEvento().isBefore(LocalDate.now().plusDays(14))) {
            throw new RuntimeException(
                    "La fecha del evento debe ser con al menos 2 semanas de anticipación desde hoy.");
        }

        if (!disponibilidadService.verificarDisponibilidadParaActualizacion(
                eventoExistente.getId(),
                eventoActualizado.getFechaEvento(),
                eventoActualizado.getHoraInicio(),
                eventoActualizado.getHoraFin())) {
            throw new RuntimeException(
                    "Ya existe un evento (APROBADO o PENDIENTE) en ese horario. Por favor, seleccione otra fecha u horario.");
        }

        actualizarCamposBasicos(eventoExistente, eventoActualizado);

        if (eventoActualizado.getResponsable() != null) {
            responsableService.actualizarResponsable(
                    eventoExistente.getResponsable(),
                    eventoActualizado.getResponsable());
        }

        if (eventoActualizado.getRequerimientos() != null) {
            requerimientoService.actualizarRequerimientos(
                    eventoExistente,
                    eventoActualizado.getRequerimientos());
        }

        return eventoRepository.save(eventoExistente);
    }

    @Transactional
    public EventoAuditorio aprobarRechazarEvento(Long id, AprobacionEventoDTO aprobacionDTO) {
        EventoAuditorio evento = obtenerEventoPorId(id);

        if (aprobacionDTO.getEstado() != EstadoEvento.APROBADO
                && aprobacionDTO.getEstado() != EstadoEvento.RECHAZADO) {
            throw new RuntimeException("El nuevo estado debe ser APROBADO o RECHAZADO");
        }

        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar/rechazar eventos en estado PENDIENTE");
        }

        if (aprobacionDTO.getEstado() == EstadoEvento.APROBADO) {
            boolean conflicto = eventoRepository.tieneConflictoHorario(
                    evento.getFechaEvento(),
                    evento.getHoraInicio(),
                    evento.getHoraFin());

            if (conflicto) {
                throw new RuntimeException("No se puede aprobar: Ya existe otro evento APROBADO en ese horario");
            }
        }

        evento.setEstado(aprobacionDTO.getEstado());
        if (aprobacionDTO.getEstado() == EstadoEvento.APROBADO
                && evento.getFechaEvento().isBefore(LocalDate.now())) {

            evento.setEstado(EstadoEvento.COMPLETADO);
        }
        evento.setMotivoRechazo(
                aprobacionDTO.getEstado() == EstadoEvento.RECHAZADO ? aprobacionDTO.getMotivoRechazo() : null);

        EventoAuditorio guardado = eventoRepository.save(evento);
        publicarCambioEstado(guardado);
        return guardado;
    }

    @Transactional
    public void eliminarEvento(Long id, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);

        verificarPermisos(evento, usuario);

        if (evento.getEstado() == EstadoEvento.APROBADO ||
                evento.getEstado() == EstadoEvento.COMPLETADO) {
            throw new RuntimeException("No se puede eliminar un evento APROBADO o COMPLETADO");
        }

        eventoRepository.delete(evento);
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
        evento.setMotivoRechazo(motivo);

        EventoAuditorio guardado = eventoRepository.save(evento);
        publicarCambioEstado(guardado);
        return guardado;
    }

    public List<EventoAuditorio> listarEventos() {
        return eventoRepository.findAll(); // ← ya no llama a actualizarEventosCompletados()
    }

    public List<EventoAuditorio> listarEventosPorUsuario(String username) {
        Usuario usuario = buscarUsuarioPorUsername(username);
        return eventoRepository.findByUsuarioSolicitanteId(usuario.getId());
    }

    public List<EventoAuditorio> listarEventosPorEstado(EstadoEvento estado) {
        return eventoRepository.findByEstado(estado);
    }

    public List<EventoAuditorio> listarEventosCompletados() {
        return eventoRepository.findByEstado(EstadoEvento.COMPLETADO);
    }

    public List<EventoAuditorio> listarEventosPorFecha(LocalDate fecha) {
        return eventoRepository.findByFechaEvento(fecha);
    }

    public EventoAuditorio obtenerEventoPorId(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Evento no encontrado con ID: " + id));
    }

    public EventoAuditorio obtenerEventoPorIdAutorizado(Long id, String username) {
        EventoAuditorio evento = obtenerEventoPorId(id);
        Usuario usuario = buscarUsuarioPorUsername(username);
        verificarPermisos(evento, usuario);
        return evento;
    }

    public List<EventoAuditorio> obtenerEventosProximos(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);

        return eventoRepository.findByEstado(EstadoEvento.APROBADO).stream()
                .filter(e -> !e.getFechaEvento().isBefore(hoy) && !e.getFechaEvento().isAfter(limite))
                .sorted((e1, e2) -> e1.getFechaEvento().compareTo(e2.getFechaEvento()))
                .toList();
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
            throw new AccessDeniedException("No tiene permiso para consultar o modificar este evento");
        }
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRole() == RolUsuario.ADMIN;
    }

    @Transactional
    public Page<EventoAuditorio> listarPaginado(
            String username,
            boolean administrador,
            String buscar,
            EstadoEvento estado,
            int pagina,
            int tamanio) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), 100);
        PageRequest pageRequest = PageRequest.of(
                paginaSegura,
                tamanioSeguro,
                Sort.by(Sort.Order.desc("fechaEvento"), Sort.Order.desc("horaInicio")));

        Specification<EventoAuditorio> filtros = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (!administrador) {
                condiciones.add(cb.equal(
                        root.get("usuarioSolicitante").get("username"), username));
            }
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (buscar != null && !buscar.isBlank()) {
                String patron = "%" + buscar.trim().toLowerCase(Locale.ROOT) + "%";
                List<Predicate> coincidencias = new ArrayList<>();
                coincidencias.add(cb.like(cb.lower(root.get("nombreEvento")), patron));
                coincidencias.add(cb.like(cb.lower(root.get("descripcion")), patron));
                coincidencias.add(cb.like(
                        cb.lower(root.join("responsable", JoinType.LEFT).get("nombre")), patron));
                if (administrador) {
                    var solicitante = root.join("usuarioSolicitante", JoinType.LEFT);
                    coincidencias.add(cb.like(cb.lower(solicitante.get("nombre")), patron));
                    coincidencias.add(cb.like(cb.lower(solicitante.get("apellido")), patron));
                }
                condiciones.add(cb.or(coincidencias.toArray(Predicate[]::new)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        return eventoRepository.findAll(filtros, pageRequest);
    }

    private void publicarCambioEstado(EventoAuditorio evento) {
        eventPublisher.publishEvent(new ReservaEstadoCambiadoEvent(evento.getId()));
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
}
