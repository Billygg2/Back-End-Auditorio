package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class CorreoReservaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorreoReservaService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final RestClient clienteBrevo;
    private final boolean habilitado;
    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;

    public CorreoReservaService(
            RestClient.Builder restClientBuilder,
            @Value("${app.mail.enabled:false}") boolean habilitado,
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String remitente,
            @Value("${app.mail.from-name:UNIB.E Reservas}") String nombreRemitente) {
        this.clienteBrevo = restClientBuilder.baseUrl("https://api.brevo.com/v3").build();
        this.habilitado = habilitado;
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    public void enviarCambioEstado(EventoAuditorio evento) {
        if (!habilitado) {
            LOGGER.info("Correo deshabilitado. No se notifico el evento {}", evento.getId());
            return;
        }

        if (evento.getUsuarioSolicitante() == null
                || evento.getUsuarioSolicitante().getCorreoInstitucional() == null) {
            LOGGER.warn("El evento {} no tiene correo de solicitante", evento.getId());
            return;
        }

        enviar(
                evento.getUsuarioSolicitante().getCorreoInstitucional(),
                "Actualización de su reserva: " + evento.getNombreEvento(),
                construirContenido(evento),
                "la notificación del evento " + evento.getId());
    }

    public void enviarNuevaReserva(EventoAuditorio evento, Usuario administrador) {
        if (!habilitado) {
            LOGGER.info("Correo deshabilitado. No se notifico la nueva reserva {}", evento.getId());
            return;
        }

        if (administrador.getCorreoInstitucional() == null
                || administrador.getCorreoInstitucional().isBlank()) {
            LOGGER.warn("El administrador {} no tiene correo registrado", administrador.getUsername());
            return;
        }

        enviar(
                administrador.getCorreoInstitucional(),
                "Nueva reserva pendiente: " + evento.getNombreEvento(),
                construirNuevaReserva(evento, administrador),
                "la nueva reserva " + evento.getId() + " al administrador " + administrador.getUsername());
    }

    private void enviar(String destinatario, String asunto, String contenidoHtml, String referencia) {
        if (apiKey.isBlank() || remitente.isBlank()) {
            LOGGER.error("Correo habilitado, pero faltan BREVO_API_KEY o MAIL_FROM. No se pudo enviar {}", referencia);
            return;
        }

        Map<String, Object> solicitud = Map.of(
                "sender", Map.of("name", nombreRemitente, "email", remitente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "htmlContent", contenidoHtml);

        try {
            clienteBrevo.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Correo enviado mediante Brevo: {}", referencia);
        } catch (RestClientException ex) {
            // El fallo del proveedor no deshace una operación ya confirmada.
            LOGGER.error("No se pudo enviar {} mediante Brevo", referencia, ex);
        }
    }

    private String construirContenido(EventoAuditorio evento) {
        String motivo = evento.getMotivoRechazo();
        String detalleMotivo = motivo == null || motivo.isBlank()
                ? ""
                : "<div style=\"margin:20px 0;padding:14px;background:#f3f6fa;border-left:4px solid #FECC0D;\">"
                    + "<strong>Motivo:</strong> " + escapar(motivo) + "</div>";

        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;background:#f3f6fa;font-family:Arial,sans-serif;color:#1C2544;">
                  <div style="max-width:620px;margin:24px auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #dce4ee;">
                    <div style="background:#1C2544;padding:24px;color:#fff;border-bottom:6px solid #FECC0D;">
                      <div style="font-size:22px;font-weight:700;">Reservas Auditorio UNIB.E</div>
                    </div>
                    <div style="padding:28px;">
                      <p>Hola, <strong>%s</strong>.</p>
                      <p>El estado de su reserva fue actualizado a:</p>
                      <div style="font-size:22px;font-weight:700;color:#004990;margin:18px 0;">%s</div>
                      <table style="width:100%%;border-collapse:collapse;line-height:1.7;">
                        <tr><td><strong>Evento</strong></td><td>%s</td></tr>
                        <tr><td><strong>Fecha</strong></td><td>%s</td></tr>
                        <tr><td><strong>Horario</strong></td><td>%s - %s</td></tr>
                      </table>
                      %s
                      <p style="margin-top:26px;color:#596579;">Este es un mensaje automatico del sistema de reservas.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapar(evento.getUsuarioSolicitante().getNombre()),
                escapar(evento.getEstado().name()),
                escapar(evento.getNombreEvento()),
                evento.getFechaEvento().format(FECHA),
                evento.getHoraInicio().format(HORA),
                evento.getHoraFin().format(HORA),
                detalleMotivo);
    }

    private String construirNuevaReserva(EventoAuditorio evento, Usuario administrador) {
        Usuario solicitante = evento.getUsuarioSolicitante();
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;background:#f3f6fa;font-family:Arial,sans-serif;color:#1C2544;">
                  <div style="max-width:620px;margin:24px auto;background:#fff;border-radius:12px;overflow:hidden;border:1px solid #dce4ee;">
                    <div style="background:#1C2544;padding:24px;color:#fff;border-bottom:6px solid #FECC0D;">
                      <div style="font-size:22px;font-weight:700;">Nueva reserva del Auditorio UNIB.E</div>
                    </div>
                    <div style="padding:28px;">
                      <p>Hola, <strong>%s</strong>.</p>
                      <p>Se registró una nueva solicitud que requiere revisión administrativa.</p>
                      <div style="font-size:22px;font-weight:700;color:#004990;margin:18px 0;">%s</div>
                      <table style="width:100%%;border-collapse:collapse;line-height:1.7;">
                        <tr><td><strong>Solicitante</strong></td><td>%s</td></tr>
                        <tr><td><strong>Fecha</strong></td><td>%s</td></tr>
                        <tr><td><strong>Horario</strong></td><td>%s - %s</td></tr>
                        <tr><td><strong>Asistentes</strong></td><td>%s</td></tr>
                      </table>
                      <p style="margin-top:26px;color:#596579;">Ingresa al sistema para revisar el detalle de la solicitud.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapar(administrador.getNombre()),
                escapar(evento.getNombreEvento()),
                escapar(solicitante.getNombre() + " " + solicitante.getApellido()),
                evento.getFechaEvento().format(FECHA),
                evento.getHoraInicio().format(HORA),
                evento.getHoraFin().format(HORA),
                evento.getNumeroAsistentes());
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
