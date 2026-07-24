package ec.edu.unibe.auditorio_backend.domain.repository;

import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);
    Optional<Usuario> findByTelefono(String telefono);
    List<Usuario> findByRole(RolUsuario role);
    List<Usuario> findByRoleAndActivoTrue(RolUsuario role);
    long countByRoleAndActivoTrue(RolUsuario role);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByCorreoInstitucionalIgnoreCaseAndIdNot(String correoInstitucional, Long id);
    boolean existsByTelefonoAndIdNot(String telefono, Long id);

}
