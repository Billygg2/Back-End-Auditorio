package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.application.dto.DisponibilidadRecursosDTO;
import ec.edu.unibe.auditorio_backend.application.dto.RecursoDisponibilidadDTO;
import ec.edu.unibe.auditorio_backend.application.dto.RequerimientoSolicitadoDTO;
import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento;
import ec.edu.unibe.auditorio_backend.domain.entity.TipoRequerimientoEntity;
import ec.edu.unibe.auditorio_backend.domain.repository.RequerimientoRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.TipoRequerimientoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RequerimientoService {

    private final RequerimientoRepository requerimientoRepository;
    private final TipoRequerimientoRepository tipoRequerimientoRepository;

    public RequerimientoService(
            RequerimientoRepository requerimientoRepository,
            TipoRequerimientoRepository tipoRequerimientoRepository) {
        this.requerimientoRepository = requerimientoRepository;
        this.tipoRequerimientoRepository = tipoRequerimientoRepository;
    }

    public void vincularRequerimientos(EventoAuditorio evento, List<Requerimiento> requerimientos) {
        if (requerimientos == null) return;

        requerimientos.forEach(r -> {
            TipoRequerimientoEntity tipo = resolverTipo(r.getTipo().getId());
            r.setTipo(tipo);
            r.setEvento(evento);
        });
    }

    public void validarDisponibilidad(
            List<Requerimiento> requerimientos,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            Long eventoId) {
        DisponibilidadRecursosDTO resultado = consultarDesdeEntidades(
                requerimientos, fecha, horaInicio, horaFin, eventoId);

        resultado.recursos().stream()
                .filter(recurso -> !recurso.disponible())
                .findFirst()
                .ifPresent(recurso -> {
                    throw new IllegalArgumentException(
                            "No hay suficiente disponibilidad de " + recurso.nombre()
                                    + " para el horario seleccionado. Disponibles: "
                                    + recurso.cantidadDisponible() + ", solicitados: "
                                    + recurso.cantidadSolicitada() + ".");
                });
    }

    public DisponibilidadRecursosDTO consultarDesdeSolicitud(
            List<RequerimientoSolicitadoDTO> solicitudes,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            Long eventoId) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        if (solicitudes != null) {
            for (RequerimientoSolicitadoDTO solicitud : solicitudes) {
                cantidades.merge(solicitud.tipoId(), solicitud.cantidad(), Integer::sum);
            }
        }
        return consultar(cantidades, fecha, horaInicio, horaFin, eventoId);
    }

    private DisponibilidadRecursosDTO consultarDesdeEntidades(
            List<Requerimiento> requerimientos,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            Long eventoId) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        if (requerimientos != null) {
            for (Requerimiento requerimiento : requerimientos) {
                if (requerimiento.getTipo() == null || requerimiento.getTipo().getId() == null) {
                    throw new IllegalArgumentException("Debe seleccionar un tipo de recurso válido");
                }
                cantidades.merge(
                        requerimiento.getTipo().getId(),
                        requerimiento.getCantidad(),
                        Integer::sum);
            }
        }
        return consultar(cantidades, fecha, horaInicio, horaFin, eventoId);
    }

    private DisponibilidadRecursosDTO consultar(
            Map<Long, Integer> cantidades,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            Long eventoId) {
        List<RecursoDisponibilidadDTO> resultados = new ArrayList<>();

        for (Map.Entry<Long, Integer> solicitud : cantidades.entrySet()) {
            TipoRequerimientoEntity tipo = resolverTipo(solicitud.getKey());
            int cantidadSolicitada = solicitud.getValue();
            if (cantidadSolicitada < 1) {
                throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que cero");
            }

            long ocupada = requerimientoRepository.cantidadReservadaEnHorario(
                    tipo.getId(), fecha, horaInicio, horaFin, eventoId);
            long disponible = Math.max(0L, tipo.getCantidadDisponible() - ocupada);

            resultados.add(new RecursoDisponibilidadDTO(
                    tipo.getId(),
                    tipo.getNombre(),
                    tipo.getCantidadDisponible(),
                    ocupada,
                    disponible,
                    cantidadSolicitada,
                    cantidadSolicitada <= disponible));
        }

        boolean disponible = resultados.stream().allMatch(RecursoDisponibilidadDTO::disponible);
        return new DisponibilidadRecursosDTO(disponible, resultados);
    }

    public void actualizarRequerimientos(EventoAuditorio evento, List<Requerimiento> requerimientosNuevos) {
        if (evento.getRequerimientos() != null && !evento.getRequerimientos().isEmpty()) {
            requerimientoRepository.deleteAll(evento.getRequerimientos());
            evento.getRequerimientos().clear();
        }

        List<Requerimiento> nuevaLista = new ArrayList<>();

        if (requerimientosNuevos != null) {
            for (Requerimiento reqNuevo : requerimientosNuevos) {
                TipoRequerimientoEntity tipo = resolverTipo(reqNuevo.getTipo().getId());

                Requerimiento req = new Requerimiento();
                req.setTipo(tipo);
                req.setCantidad(reqNuevo.getCantidad());
                req.setRequerido(reqNuevo.isRequerido());
                req.setEvento(evento);
                nuevaLista.add(req);
            }
        }

        evento.setRequerimientos(nuevaLista);
    }

    private TipoRequerimientoEntity resolverTipo(Long tipoId) {
        TipoRequerimientoEntity tipo = tipoRequerimientoRepository.findById(tipoId)
                .orElseThrow(() -> new RuntimeException(
                        "Tipo de requerimiento no encontrado: " + tipoId));
        if (!tipo.isActivo()) {
            throw new IllegalArgumentException("El recurso " + tipo.getNombre() + " está inactivo");
        }
        return tipo;
    }
}
