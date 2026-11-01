package br.com.openoffice.repository;

import br.com.openoffice.model.Notificacao;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository extends RepositorioJson<Notificacao> {

    public NotificacaoRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "notificacoes.json", Notificacao.class, "N");
    }

    public List<Notificacao> doMembro(String membroId) {
        return filtrar(n -> n.getMembroId().equals(membroId)).stream()
                .sorted(Comparator.comparing(Notificacao::getCriadaEm).reversed())
                .toList();
    }
}
