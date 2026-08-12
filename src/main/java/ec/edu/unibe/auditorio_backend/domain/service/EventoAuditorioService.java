package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.application.dto.AprobacionEventoDTO;
import ec.edu.unibe.auditorio_backend.application.dto.DocumentoAprobacionDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import ec.edu.unibe.auditorio_backend.domain.entity.Responsable;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.NotificacionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled; // ← NUEVO IMPORT
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.util.Set;

@Service
public class EventoAuditorioService {

    private static final ZoneId ZONA_ECUADOR = ZoneId.of("America/Guayaquil");
    private static final long TAMANIO_MAXIMO_DOCUMENTO = 10L * 1024L * 1024L;
    private static final Set<String> TIPOS_DOCUMENTO_PERMITIDOS = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png");

    private final EventoAuditorioRepository eventoRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadService disponibilidadService;
    private final RequerimientoService requerimientoService;
    private final ResponsableService responsableService;
    private final EspacioService espacioService;
    private final ApplicationEventPublisher eventPublisher;

    public EventoAuditorioService(
            EventoAuditorioRepository eventoRepository,
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            DisponibilidadService disponibilidadService,
            RequerimientoService requerimientoService,
            ResponsableService responsableService,
            EspacioService espacioService,
            ApplicationEventPublisher eventPublisher) {
        this.eventoRepository = eventoRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.disponibilidadService = disponibilidadService;
        this.requerimientoService = requerimientoService;
        this.responsableService = responsableService;
        this.espacioService = espacioService;
        this.eventPublisher = eventPublisher;
    }

    // ==================== SCHEDULER AUTOMÁTICO ====================
    /**
     * Revisa cada minuto los eventos aprobados y los completa cuando alcanza
     * su fecha y hora de finalización en el horario de Ecuador.
     */
    @PostConstruct
    @Scheduled(cron = "0 * * * * *", zone = "America/Guayaquil")
    @Transactional
    public void marcarEventosCompletadosAutomaticamente() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_ECUADOR);

        List<EventoAuditorio> eventosAprobados = eventoRepository.findByEstado(EstadoEvento.APROBADO);

        List<EventoAuditorio> aCompletar = eventosAprobados.stream()
                .filter(evento -> eventoYaFinalizo(evento, ahora))
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
        Espacio espacio = resolverEspacio(evento.getEspacio());
        evento.setEspacio(espacio);
        normalizarDatosPublico(evento);
        validarDatosEvento(evento, espacio);

        // ← NUEVO: validar mínimo 2 semanas de anticipación
        if (evento.getFechaEvento().isBefore(calcularFechaMinimaReserva())) {
            throw new RuntimeException(
                    "La fecha del evento debe tener al menos 3 días hábiles de anticipación.");
        }

        disponibilidadService.bloquearFecha(evento.getFechaEvento());

        if (!disponibilidadService.verificarDisponibilidad(
                espacio.getId(), evento.getFechaEvento(), evento.getHoraInicio(), evento.getHoraFin())) {
            throw new RuntimeException(
                    "El espacio no está disponible en ese horario. Debe existir 1 hora de preparación entre eventos.");
        }

        requerimientoService.validarDisponibilidad(
                evento.getRequerimientos(),
                evento.getFechaEvento(),
                evento.getHoraInicio(),
                evento.getHoraFin(),
                null);

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

        Espacio espacio = eventoActualizado.getEspacio() == null
                ? eventoExistente.getEspacio()
                : resolverEspacio(eventoActualizado.getEspacio());
        eventoActualizado.setEspacio(espacio);
        normalizarDatosPublico(eventoActualizado);
        validarDatosEvento(eventoActualizado, espacio);

        // ← NUEVO: validar mínimo 2 semanas también al actualizar
        if (eventoActualizado.getFechaEvento().isBefore(calcularFechaMinimaReserva())) {
            throw new RuntimeException(
                    "La fecha del evento debe tener al menos 3 días hábiles de anticipación.");
        }

        disponibilidadService.bloquearFecha(eventoActualizado.getFechaEvento());

        if (!disponibilidadService.verificarDisponibilidadParaActualizacion(
                eventoExistente.getId(),
                espacio.getId(),
                eventoActualizado.getFechaEvento(),
                eventoActualizado.getHoraInicio(),
                eventoActualizado.getHoraFin())) {
            throw new RuntimeException(
                    "El espacio no está disponible en ese horario. Debe existir 1 hora de preparación entre eventos.");
        }

        List<ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento> recursosAValidar =
                eventoActualizado.getRequerimientos() != null
                        ? eventoActualizado.getRequerimientos()
                        : eventoExistente.getRequerimientos();
        requerimientoService.validarDisponibilidad(
                recursosAValidar,
                eventoActualizado.getFechaEvento(),
                eventoActualizado.getHoraInicio(),
                eventoActualizado.getHoraFin(),
                eventoExistente.getId());

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
            disponibilidadService.bloquearFecha(evento.getFechaEvento());

            requerimientoService.validarDisponibilidad(
                    evento.getRequerimientos(),
                    evento.getFechaEvento(),
                    evento.getHoraInicio(),
                    evento.getHoraFin(),
                    evento.getId());

            boolean conflicto = !disponibilidadService.verificarDisponibilidadParaActualizacion(
                    evento.getId(),
                    evento.getEspacio().getId(),
                    evento.getFechaEvento(),
                    evento.getHoraInicio(),
                    evento.getHoraFin());

            if (conflicto) {
                throw new RuntimeException(
                        "No se puede aprobar: debe existir 1 hora de preparación entre eventos del mismo espacio");
            }
        }

        evento.setEstado(aprobacionDTO.getEstado());
        if (aprobacionDTO.getEstado() == EstadoEvento.APROBADO
                && eventoYaFinalizo(evento, LocalDateTime.now(ZONA_ECUADOR))) {

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

        // Conserva el historial de notificaciones, pero elimina el enlace a
        // una reserva que dejará de existir.
        notificacionRepository.desvincularEvento(id);
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

    public List<EventoAuditorio> listarEventosPorEstadoYEspacio(EstadoEvento estado, Long espacioId) {
        return eventoRepository.findByEstadoAndEspacioId(estado, espacioId);
    }

    public List<EventoAuditorio> listarEventosCompletados() {
        return eventoRepository.findByEstado(EstadoEvento.COMPLETADO);
    }

    public List<EventoAuditorio> listarEventosPorFecha(LocalDate fecha) {
        return eventoRepository.findByFechaEvento(fecha);
    }

    public List<EventoAuditorio> listarEventosPorFechaYEspacio(LocalDate fecha, Long espacioId) {
        return eventoRepository.findByFechaEventoAndEspacioId(fecha, espacioId);
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

    @Transactional
    public EventoAuditorio guardarDocumentoAprobacion(
            Long eventoId,
            MultipartFile archivo,
            String username) {
        EventoAuditorio evento = obtenerEventoPorId(eventoId);
        Usuario usuario = buscarUsuarioPorUsername(username);
        verificarPermisos(evento, usuario);
        verificarReservaEditableParaDocumento(evento);
        validarDocumento(archivo);

        try {
            evento.setDocumentoAprobacionNombre(limpiarNombreArchivo(archivo.getOriginalFilename()));
            evento.setDocumentoAprobacionTipo(archivo.getContentType());
            evento.setDocumentoAprobacionTamanio(archivo.getSize());
            evento.setDocumentoAprobacionContenido(archivo.getBytes());
            return eventoRepository.save(evento);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el documento de aprobación");
        }
    }

    @Transactional(readOnly = true)
    public DocumentoAprobacionDTO descargarDocumentoAprobacion(Long eventoId, String username) {
        EventoAuditorio evento = obtenerEventoPorIdAutorizado(eventoId, username);
        if (!evento.isTieneDocumentoAprobacion()) {
            throw new NoSuchElementException("La reserva no tiene un documento de aprobación");
        }
        return new DocumentoAprobacionDTO(
                evento.getDocumentoAprobacionNombre(),
                evento.getDocumentoAprobacionTipo(),
                evento.getDocumentoAprobacionTamanio(),
                evento.getDocumentoAprobacionContenido());
    }

    @Transactional
    public void eliminarDocumentoAprobacion(Long eventoId, String username) {
        EventoAuditorio evento = obtenerEventoPorId(eventoId);
        Usuario usuario = buscarUsuarioPorUsername(username);
        verificarPermisos(evento, usuario);
        verificarReservaEditableParaDocumento(evento);
        evento.setDocumentoAprobacionNombre(null);
        evento.setDocumentoAprobacionTipo(null);
        evento.setDocumentoAprobacionTamanio(null);
        evento.setDocumentoAprobacionContenido(null);
        eventoRepository.save(evento);
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
            Long espacioId,
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
            if (espacioId != null) {
                condiciones.add(cb.equal(root.get("espacio").get("id"), espacioId));
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
        existente.setEspacio(actualizado.getEspacio());
        existente.setEmpresaPublicoExterno(
                actualizado.isPublicoExterno() ? normalizar(actualizado.getEmpresaPublicoExterno()) : null);
        existente.setPublicoInterno(actualizado.isPublicoInterno());
        existente.setCarreraPublicoInterno(
                actualizado.isPublicoInterno() ? normalizar(actualizado.getCarreraPublicoInterno()) : null);
    }

    private boolean eventoYaFinalizo(EventoAuditorio evento, LocalDateTime ahora) {
        LocalDateTime finalizacion = LocalDateTime.of(
                evento.getFechaEvento(),
                evento.getHoraFin());
        return !finalizacion.isAfter(ahora);
    }

    private Espacio resolverEspacio(Espacio espacioSolicitado) {
        if (espacioSolicitado == null || espacioSolicitado.getId() == null) {
            return espacioService.obtenerPredeterminado();
        }
        return espacioService.obtenerActivo(espacioSolicitado.getId());
    }

    private void validarAforo(EventoAuditorio evento, Espacio espacio) {
        if (evento.getNumeroAsistentes() > espacio.getAforo()) {
            throw new IllegalArgumentException(
                    "El número de asistentes supera el aforo de " + espacio.getAforo()
                            + " personas del espacio seleccionado");
        }
    }

    private void validarDatosEvento(EventoAuditorio evento, Espacio espacio) {
        if (evento.getNombreEvento() == null
                || evento.getNombreEvento().isBlank()
                || evento.getNombreEvento().trim().length() < 5
                || evento.getNombreEvento().trim().length() > 200) {
            throw new IllegalArgumentException("El nombre del evento debe tener entre 5 y 200 caracteres");
        }
        evento.setNombreEvento(evento.getNombreEvento().trim());
        if (evento.getFechaEvento() == null
                || evento.getHoraInicio() == null
                || evento.getHoraFin() == null) {
            throw new IllegalArgumentException("La fecha y el horario son obligatorios");
        }
        if (!evento.getHoraInicio().isBefore(evento.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de finalización");
        }
        if (evento.getNumeroAsistentes() < 1) {
            throw new IllegalArgumentException("Debe existir al menos un asistente");
        }
        if (evento.getTipoDisposicion() == null || evento.getTipoDisposicion().isBlank()) {
            throw new IllegalArgumentException("La distribución del espacio es obligatoria");
        }
        if (evento.getResponsable() == null) {
            throw new IllegalArgumentException("La persona responsable es obligatoria");
        }
        if (evento.isPublicoExterno()
                && (evento.getEmpresaPublicoExterno() == null
                    || evento.getEmpresaPublicoExterno().isBlank())) {
            throw new IllegalArgumentException("Debe indicar la empresa del público externo");
        }
        if (evento.isPublicoInterno()
                && (evento.getCarreraPublicoInterno() == null
                    || evento.getCarreraPublicoInterno().isBlank())) {
            throw new IllegalArgumentException("Debe indicar la carrera del público interno");
        }
        validarAforo(evento, espacio);
    }

    private void normalizarDatosPublico(EventoAuditorio evento) {
        evento.setEmpresaPublicoExterno(
                evento.isPublicoExterno() ? normalizar(evento.getEmpresaPublicoExterno()) : null);
        evento.setCarreraPublicoInterno(
                evento.isPublicoInterno() ? normalizar(evento.getCarreraPublicoInterno()) : null);
    }

    /**
     * Calcula la primera fecha permitida contando únicamente de lunes a
     * viernes. El día actual no se incluye dentro de los tres días hábiles.
     */
    private LocalDate calcularFechaMinimaReserva() {
        LocalDate fecha = LocalDate.now(ZONA_ECUADOR);
        int diasHabiles = 0;
        while (diasHabiles < 3) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY
                    && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                diasHabiles++;
            }
        }
        return fecha;
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private void validarDocumento(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un documento de aprobación");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_DOCUMENTO) {
            throw new IllegalArgumentException("El documento no puede superar 10 MB");
        }
        if (archivo.getContentType() == null
                || !TIPOS_DOCUMENTO_PERMITIDOS.contains(archivo.getContentType())) {
            throw new IllegalArgumentException(
                    "Formato no permitido. Utilice PDF, Word, JPG o PNG");
        }
    }

    private void verificarReservaEditableParaDocumento(EventoAuditorio evento) {
        if (evento.getEstado() != EstadoEvento.PENDIENTE) {
            throw new IllegalArgumentException(
                    "El documento solo puede modificarse mientras la reserva está pendiente");
        }
    }

    private String limpiarNombreArchivo(String nombreOriginal) {
        String nombre = nombreOriginal == null ? "documento-aprobacion" : nombreOriginal;
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        return nombre.isBlank() ? "documento-aprobacion" : nombre;
    }
}
