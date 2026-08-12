package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class DisponibilidadService {

    private static final int MINUTOS_PREPARACION = 60;

    private final EventoAuditorioRepository eventoRepository;
    private final EspacioService espacioService;

    public DisponibilidadService(
            EventoAuditorioRepository eventoRepository,
            EspacioService espacioService) {
        this.eventoRepository = eventoRepository;
        this.espacioService = espacioService;
    }

    /**
     * El bloqueo se libera automáticamente al confirmar o revertir la
     * transacción que realiza la reserva.
     */
    public void bloquearFecha(LocalDate fecha) {
        eventoRepository.bloquearFechaParaReserva(fecha.toEpochDay());
    }

    public boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        return verificarDisponibilidad(
                espacioService.obtenerPredeterminado().getId(),
                fecha,
                horaInicio,
                horaFin);
    }

    public boolean verificarDisponibilidad(
            Long espacioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        espacioService.obtenerActivo(espacioId);
        LocalTime inicioConPreparacion = horaInicio.minusMinutes(MINUTOS_PREPARACION);
        LocalTime finConPreparacion = horaFin.plusMinutes(MINUTOS_PREPARACION);
        return !eventoRepository.tieneConflictoHorarioEnEspacio(
                espacioId, fecha, inicioConPreparacion, finConPreparacion, null);
    }

    public boolean verificarDisponibilidadParaActualizacion(
            Long eventoId,
            Long espacioId,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        espacioService.obtenerActivo(espacioId);
        LocalTime inicioConPreparacion = horaInicio.minusMinutes(MINUTOS_PREPARACION);
        LocalTime finConPreparacion = horaFin.plusMinutes(MINUTOS_PREPARACION);
        return !eventoRepository.tieneConflictoHorarioEnEspacio(
                espacioId, fecha, inicioConPreparacion, finConPreparacion, eventoId);
    }
}
