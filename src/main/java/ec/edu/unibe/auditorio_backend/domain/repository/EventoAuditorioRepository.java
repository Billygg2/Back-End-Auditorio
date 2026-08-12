package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoAuditorioRepository extends JpaRepository<EventoAuditorio, Long>, JpaSpecificationExecutor<EventoAuditorio> {

    /**
     * Serializa las operaciones de reserva de una misma fecha hasta finalizar
     * la transacción actual. Evita que dos solicitudes simultáneas validen el
     * mismo horario como disponible antes de que alguna sea confirmada.
     */
    @Query(value = "SELECT :claveFecha FROM pg_advisory_xact_lock(:claveFecha)", nativeQuery = true)
    long bloquearFechaParaReserva(@Param("claveFecha") long claveFecha);

    @Modifying
    @Query("UPDATE EventoAuditorio e SET e.espacio = :espacio WHERE e.espacio IS NULL")
    int asignarEspacioAEventosSinEspacio(@Param("espacio") Espacio espacio);
    
    List<EventoAuditorio> findByFechaEvento(LocalDate fechaEvento);

    List<EventoAuditorio> findByFechaEventoAndEspacioId(LocalDate fechaEvento, Long espacioId);
    
    List<EventoAuditorio> findByUsuarioSolicitanteId(Long usuarioId);

    List<EventoAuditorio> findByEstado(EstadoEvento estado);

    List<EventoAuditorio> findByEstadoAndEspacioId(EstadoEvento estado, Long espacioId);

    
    List<EventoAuditorio> findByFechaEventoAndEstado(LocalDate fechaEvento, EstadoEvento estado);
    
    @Query("SELECT e FROM EventoAuditorio e WHERE e.responsable.correo LIKE %:email%")
    List<EventoAuditorio> findByEmailResponsable(@Param("email") String email);
    
    @Query("SELECT e FROM EventoAuditorio e WHERE e.responsable.id = :responsableId")
    List<EventoAuditorio> findByResponsableId(@Param("responsableId") Long responsableId);
    
    boolean existsByFechaEventoAndHoraInicioLessThanAndHoraFinGreaterThan(
            LocalDate fechaEvento,
            LocalTime horaFin,
            LocalTime horaInicio
    );
    
    @Query("SELECT COUNT(e) > 0 FROM EventoAuditorio e " +
           "WHERE e.espacio.id = :espacioId AND e.fechaEvento = :fecha " +
           "AND e.estado IN ('PENDIENTE', 'APROBADO') " +
           "AND (:eventoId IS NULL OR e.id <> :eventoId) " +
           "AND e.horaInicio < :horaFin AND e.horaFin > :horaInicio")
    boolean tieneConflictoHorarioEnEspacio(
            @Param("espacioId") Long espacioId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("eventoId") Long eventoId);
}
