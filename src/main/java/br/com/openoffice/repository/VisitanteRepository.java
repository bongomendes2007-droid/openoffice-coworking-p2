package br.com.openoffice.repository;

import br.com.openoffice.model.pessoa.Visitante;
import br.com.openoffice.util.Validador;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class VisitanteRepository extends RepositorioJson<Visitante> {

    public VisitanteRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "visitantes.json", Visitante.class, "T");
    }

    public Optional<Visitante> buscarPorDocumento(String documento) {
        String alvo = Validador.normalizarDocumento(documento);
        return filtrar(v -> v.getDocumentoNormalizado().equals(alvo)).stream().findFirst();
    }
}
