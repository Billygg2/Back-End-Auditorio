package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DisponibilidadService {

    private final EventoAuditorioRepository eventoRepository;

    public DisponibilidadService(EventoAuditorioRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        boolean conflictoAprobados = eventoRepository.tieneConflictoHorario(fecha, horaInicio, horaFin);

        boolean conflictoPendientes = eventoRepository.findByEstado(EstadoEvento.PENDIENTE).stream()
                .filter(e -> e.getFechaEvento().equals(fecha))
                .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));

        return !conflictoAprobados && !conflictoPendientes;
    }

    public boolean verificarDisponibilidadParaActualizacion(Long eventoId, LocalDate fecha,
            LocalTime horaInicio, LocalTime horaFin) {

        List<EventoAuditorio> eventosAprobados = eventoRepository.findByEstado(EstadoEvento.APROBADO);
        boolean conflictoAprobados = eventosAprobados.stream()
                .filter(e -> !e.getId().equals(eventoId))
                .filter(e -> e.getFechaEvento().equals(fecha))
                .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));

        List<EventoAuditorio> eventosPendientes = eventoRepository.findByEstado(EstadoEvento.PENDIENTE);
        boolean conflictoPendientes = eventosPendientes.stream()
                .filter(e -> !e.getId().equals(eventoId))
                .filter(e -> e.getFechaEvento().equals(fecha))
                .anyMatch(e -> hayConflictoHorario(e.getHoraInicio(), e.getHoraFin(), horaInicio, horaFin));

        return !conflictoAprobados && !conflictoPendientes;
    }

    public boolean hayConflictoHorario(LocalTime inicio1, LocalTime fin1,
            LocalTime inicio2, LocalTime fin2) {
        return inicio2.isBefore(fin1) && fin2.isAfter(inicio1);
    }
}