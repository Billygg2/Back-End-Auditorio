package ec.edu.unibe.auditorio_backend.application.controller;

import ec.edu.unibe.auditorio_backend.application.auth.AuthRequest;
import ec.edu.unibe.auditorio_backend.application.auth.AuthResponse;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import ec.edu.unibe.auditorio_backend.domain.repository.UsuarioRepository;
import ec.edu.unibe.auditorio_backend.infrastructure.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600) 
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, 
                         AuthenticationManager authenticationManager,
                         JwtUtil jwtUtil,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request, BindingResult result) {
        
        // Validar errores de validación
        if (result.hasErrors()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errors", result.getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .collect(Collectors.toList()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Verificar si el username (cédula) ya existe
        Optional<Usuario> existente = usuarioRepository.findByUsername(request.getUsername());
        if (existente.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "La cédula ya está registrada");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Verificar si el correo institucional ya existe
        Optional<Usuario> existentePorCorreo = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());
        if (existentePorCorreo.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "El correo institucional ya está registrado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Crear nuevo usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsername(request.getUsername());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRole(request.getRole() != null ? request.getRole() : "USER");
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setCorreoInstitucional(request.getCorreoInstitucional());
        nuevoUsuario.setTelefono(request.getTelefono());
        
        usuarioRepository.save(nuevoUsuario);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario registrado correctamente");
        response.put("username", nuevoUsuario.getUsername());
        response.put("nombre", nuevoUsuario.getNombre());
        response.put("apellido", nuevoUsuario.getApellido());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AuthRequest request, BindingResult result) {
        
        // Validar errores de validación
        if (result.hasErrors()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errors", result.getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .collect(Collectors.toList()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Verificar si el username (cédula) ya existe
        Optional<Usuario> existente = usuarioRepository.findByUsername(request.getUsername());
        if (existente.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "La cédula ya está registrada");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Verificar si el correo institucional ya existe
        Optional<Usuario> existentePorCorreo = usuarioRepository.findByCorreoInstitucional(request.getCorreoInstitucional());
        if (existentePorCorreo.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "El correo institucional ya está registrado");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Crear nuevo usuario con rol ADMIN (ignoramos el role del request)
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsername(request.getUsername());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRole("ADMIN");  // Siempre ADMIN
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setCorreoInstitucional(request.getCorreoInstitucional());
        nuevoUsuario.setTelefono(request.getTelefono());
        
        usuarioRepository.save(nuevoUsuario);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Administrador registrado correctamente");
        response.put("username", nuevoUsuario.getUsername());
        response.put("nombre", nuevoUsuario.getNombre());
        response.put("apellido", nuevoUsuario.getApellido());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(), 
                    request.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generarToken(userDetails);

            return ResponseEntity.ok(new AuthResponse(token));
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Credenciales incorrectas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}