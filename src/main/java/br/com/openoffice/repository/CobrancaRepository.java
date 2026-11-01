package br.com.openoffice.repository;

import br.com.openoffice.model.financeiro.Cobranca;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CobrancaRepository extends RepositorioJson<Cobranca> {

    public CobrancaRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "cobrancas.json", Cobranca.class, "C");
    }

    public List<Cobranca> doMembro(String membroId) {
        return filtrar(c -> c.getMembroId().equals(membroId));
    }

    public List<Cobranca> pendentesDoMembro(String membroId) {
        return filtrar(c -> c.getMembroId().equals(membroId) && c.isPendente());
    }
}
