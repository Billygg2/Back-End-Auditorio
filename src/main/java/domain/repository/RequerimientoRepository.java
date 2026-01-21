package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequerimientoRepository 
        extends JpaRepository<Requerimiento, Long> {
}
