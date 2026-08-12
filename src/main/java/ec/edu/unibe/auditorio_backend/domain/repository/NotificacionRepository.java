package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioIdOrderByCreadaEnDesc(Long destinatarioId);
    long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);
    Optional<Notificacion> findByIdAndDestinatarioId(Long id, Long destinatarioId);
    List<Notificacion> findByDestinatarioIdAndLeidaFalse(Long destinatarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.evento = null WHERE n.evento.id = :eventoId")
    int desvincularEvento(@Param("eventoId") Long eventoId);
}
