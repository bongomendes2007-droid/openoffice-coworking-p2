package br.com.openoffice.repository;

import br.com.openoffice.model.visita.Visita;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class VisitaRepository extends RepositorioJson<Visita> {

    public VisitaRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "visitas.json", Visita.class, "VS");
    }

    public List<Visita> emAndamentoDoMembro(String membroId) {
        return filtrar(v -> v.isEmAndamento() && v.getMembroId().equals(membroId));
    }

    public List<Visita> emAndamento() {
        return filtrar(Visita::isEmAndamento);
    }

    public List<Visita> doMembro(String membroId) {
        return filtrar(v -> v.getMembroId().equals(membroId));
    }
}
