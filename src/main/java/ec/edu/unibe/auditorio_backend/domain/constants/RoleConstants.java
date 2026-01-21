package ec.edu.unibe.auditorio_backend.domain.constants;

public final class RoleConstants {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    
    private RoleConstants() {
        throw new IllegalStateException("Clase de constantes");
    }
}