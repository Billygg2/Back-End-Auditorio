package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioIdOrderByCreadaEnDesc(Long destinatarioId);
    long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);
    Optional<Notificacion> findByIdAndDestinatarioId(Long id, Long destinatarioId);
    List<Notificacion> findByDestinatarioIdAndLeidaFalse(Long destinatarioId);
}
