package br.com.openoffice.repository;

import br.com.openoffice.model.financeiro.Pagamento;
import org.springframework.stereotype.Repository;

@Repository
public class PagamentoRepository extends RepositorioJson<Pagamento> {

    public PagamentoRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "pagamentos.json", Pagamento.class, "P");
    }
}
