package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.enums.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoAuditorioRepository extends JpaRepository<EventoAuditorio, Long> {
    
    List<EventoAuditorio> findByFechaEvento(LocalDate fechaEvento);
    
    List<EventoAuditorio> findByUsuarioSolicitanteId(Long usuarioId);

    List<EventoAuditorio> findByEstado(EstadoEvento estado);
    
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
    
    @Query("SELECT COUNT(e) > 0 FROM EventoAuditorio e WHERE e.fechaEvento = :fecha " +
           "AND e.estado = 'APROBADO' " +
           "AND ((e.horaInicio < :horaFin AND e.horaFin > :horaInicio))")
    boolean tieneConflictoHorario(
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );
}