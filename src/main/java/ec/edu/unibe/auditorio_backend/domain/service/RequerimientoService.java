package ec.edu.unibe.auditorio_backend.domain.service;

import ec.edu.unibe.auditorio_backend.domain.entity.EventoAuditorio;
import ec.edu.unibe.auditorio_backend.domain.entity.Requerimiento;
import ec.edu.unibe.auditorio_backend.domain.entity.TipoRequerimientoEntity;
import ec.edu.unibe.auditorio_backend.domain.repository.RequerimientoRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.TipoRequerimientoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        return tipoRequerimientoRepository.findById(tipoId)
                .orElseThrow(() -> new RuntimeException(
                        "Tipo de requerimiento no encontrado: " + tipoId));
    }
}