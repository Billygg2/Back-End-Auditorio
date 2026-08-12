package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    List<Espacio> findByActivoTrueOrderByNombreAsc();
    Optional<Espacio> findByCodigo(String codigo);
}
