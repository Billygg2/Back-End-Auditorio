package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface RequerimientoRepository 
        extends JpaRepository<Requerimiento, Long> {

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Requerimiento r " +
           "JOIN r.evento e " +
           "WHERE r.tipo.id = :tipoId " +
           "AND e.fechaEvento = :fecha " +
           "AND e.estado IN ('PENDIENTE', 'APROBADO') " +
           "AND (:eventoId IS NULL OR e.id <> :eventoId) " +
           "AND e.horaInicio < :horaFin AND e.horaFin > :horaInicio")
    long cantidadReservadaEnHorario(
            @Param("tipoId") Long tipoId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("eventoId") Long eventoId);
}
