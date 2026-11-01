package br.com.openoffice.repository;

import br.com.openoffice.model.evento.Evento;
import org.springframework.stereotype.Repository;

@Repository
public class EventoRepository extends RepositorioJson<Evento> {

    public EventoRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "eventos.json", Evento.class, "V");
    }
}
