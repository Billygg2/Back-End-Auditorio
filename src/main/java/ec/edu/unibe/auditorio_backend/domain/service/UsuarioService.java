package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.application.dto.*;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.enums.RolUsuario;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ArrayList;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar(String buscar) {
        String criterio = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);

        return usuarioRepository.findAll().stream()
                .filter(usuario -> coincideBusqueda(usuario, criterio))
                .sorted(Comparator.comparing(Usuario::getApellido)
                        .thenComparing(Usuario::getNombre))
                .map(this::aDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDTO> listarPaginado(String buscar, int pagina, int tamanio) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), 100);
        PageRequest pageRequest = PageRequest.of(
                paginaSegura,
                tamanioSeguro,
                Sort.by(Sort.Order.asc("apellido"), Sort.Order.asc("nombre")));

        Specification<Usuario> filtros = (root, query, cb) -> {
            if (buscar == null || buscar.isBlank()) return cb.conjunction();
            String patron = "%" + buscar.trim().toLowerCase(Locale.ROOT) + "%";
            List<Predicate> coincidencias = new ArrayList<>();
            coincidencias.add(cb.like(cb.lower(root.get("username")), patron));
            coincidencias.add(cb.like(cb.lower(root.get("nombre")), patron));
            coincidencias.add(cb.like(cb.lower(root.get("apellido")), patron));
            coincidencias.add(cb.like(cb.lower(root.get("correoInstitucional")), patron));
            coincidencias.add(cb.like(cb.lower(root.get("telefono")), patron));
            return cb.or(coincidencias.toArray(Predicate[]::new));
        };

        return usuarioRepository.findAll(filtros, pageRequest).map(this::aDTO);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtener(Long id) {
        return aDTO(buscarPorId(id));
    }

    @Transactional
    public UsuarioDTO actualizarComoAdmin(
            Long id,
            ActualizarUsuarioAdminDTO datos,
            String usernameAdministrador) {
        Usuario usuario = buscarPorId(id);
        boolean esMismaCuenta = usuario.getUsername().equals(usernameAdministrador);

        validarDatosUnicos(id, datos.username(), datos.correoInstitucional(), datos.telefono());

        if (esMismaCuenta && !usuario.getUsername().equals(datos.username())) {
            throw new IllegalArgumentException("No puedes modificar tu propia cédula mientras tienes una sesión activa");
        }
        if (esMismaCuenta && datos.role() != RolUsuario.ADMIN) {
            throw new IllegalArgumentException("No puedes quitarte tu propio rol de administrador");
        }
        if (esMismaCuenta && !datos.activo()) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }

        validarQuePermanezcaUnAdministrador(usuario, datos.role(), datos.activo());

        usuario.setUsername(datos.username());
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setCorreoInstitucional(datos.correoInstitucional().trim());
        usuario.setTelefono(datos.telefono());
        usuario.setRole(datos.role());
        usuario.setActivo(datos.activo());

        return aDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO cambiarEstado(Long id, boolean activo, String usernameAdministrador) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getUsername().equals(usernameAdministrador) && !activo) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }

        validarQuePermanezcaUnAdministrador(usuario, usuario.getRole(), activo);
        usuario.setActivo(activo);
        return aDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void restablecerPassword(Long id, RestablecerPasswordDTO datos) {
        Usuario usuario = buscarPorId(id);
        usuario.setPassword(passwordEncoder.encode(datos.nuevaPassword()));
        usuario.setDebeCambiarPassword(true);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerMiCuenta(String username) {
        return aDTO(buscarPorUsername(username));
    }

    @Transactional
    public UsuarioDTO actualizarMiTelefono(String username, ActualizarTelefonoDTO datos) {
        Usuario usuario = buscarPorUsername(username);
        if (usuarioRepository.existsByTelefonoAndIdNot(datos.telefono(), usuario.getId())) {
            throw new IllegalArgumentException("El número de teléfono ya está registrado");
        }
        usuario.setTelefono(datos.telefono());
        return aDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void cambiarMiPassword(String username, CambiarPasswordDTO datos) {
        Usuario usuario = buscarPorUsername(username);
        if (!passwordEncoder.matches(datos.passwordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(datos.nuevaPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(datos.nuevaPassword()));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
    }

    private void validarDatosUnicos(Long id, String username, String correo, String telefono) {
        if (usuarioRepository.existsByUsernameAndIdNot(username, id)) {
            throw new IllegalArgumentException("La cédula ya está registrada");
        }
        if (usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot(correo, id)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByTelefonoAndIdNot(telefono, id)) {
            throw new IllegalArgumentException("El número de teléfono ya está registrado");
        }
    }

    private void validarQuePermanezcaUnAdministrador(
            Usuario usuario,
            RolUsuario nuevoRol,
            boolean nuevoEstadoActivo) {
        boolean dejaDeSerAdminActivo = usuario.getRole() == RolUsuario.ADMIN
                && usuario.isActivo()
                && (nuevoRol != RolUsuario.ADMIN || !nuevoEstadoActivo);

        if (dejaDeSerAdminActivo
                && usuarioRepository.countByRoleAndActivoTrue(RolUsuario.ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe permanecer al menos un administrador activo");
        }
    }

    private boolean coincideBusqueda(Usuario usuario, String criterio) {
        if (criterio.isBlank()) return true;
        return usuario.getUsername().contains(criterio)
                || usuario.getNombre().toLowerCase(Locale.ROOT).contains(criterio)
                || usuario.getApellido().toLowerCase(Locale.ROOT).contains(criterio)
                || usuario.getCorreoInstitucional().toLowerCase(Locale.ROOT).contains(criterio)
                || usuario.getTelefono().contains(criterio)
                || usuario.getRole().name().toLowerCase(Locale.ROOT).contains(criterio);
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    private Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getNombre() + " " + usuario.getApellido(),
                usuario.getCorreoInstitucional(),
                usuario.getTelefono(),
                usuario.getRole(),
                usuario.isActivo(),
                usuario.isDebeCambiarPassword());
    }
}
