package ec.edu.unibe.auditorio_backend.infrastructure.config;

import ec.edu.unibe.auditorio_backend.domain.entity.Espacio;
import ec.edu.unibe.auditorio_backend.domain.repository.EspacioRepository;
import ec.edu.unibe.auditorio_backend.domain.repository.EventoAuditorioRepository;
import ec.edu.unibe.auditorio_backend.domain.service.EspacioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class EspacioDataInitializer implements ApplicationRunner {

    private final EspacioRepository espacioRepository;
    private final EventoAuditorioRepository eventoRepository;

    public EspacioDataInitializer(
            EspacioRepository espacioRepository,
            EventoAuditorioRepository eventoRepository) {
        this.espacioRepository = espacioRepository;
        this.eventoRepository = eventoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Espacio> configuracion = List.of(
                new Espacio("AUD_B6_P1", "Auditorio", "Bloque 6", "Piso 1", 140),
                new Espacio("SALA_B6_P1", "Sala de reuniones", "Bloque 6", "Piso 1", 60),
                new Espacio("SALA_B6_P2", "Sala de reuniones", "Bloque 6", "Piso 2", 60),
                new Espacio("SALA_B7_PB", "Sala de reuniones", "Bloque 7", "Planta baja", 10));

        for (Espacio espacio : configuracion) {
            espacioRepository.findByCodigo(espacio.getCodigo())
                    .orElseGet(() -> espacioRepository.save(espacio));
        }

        Espacio auditorio = espacioRepository
                .findByCodigo(EspacioService.CODIGO_AUDITORIO_PRINCIPAL)
                .orElseThrow();
        eventoRepository.asignarEspacioAEventosSinEspacio(auditorio);
    }
}
