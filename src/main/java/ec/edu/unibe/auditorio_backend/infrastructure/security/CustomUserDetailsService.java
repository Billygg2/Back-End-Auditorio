package ec.edu.unibe.auditorio_backend.infrastructure.security;

import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public CustomUserDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no existe"));

        String role = "ROLE_" + usuario.getRole().name();

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isActivo(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
