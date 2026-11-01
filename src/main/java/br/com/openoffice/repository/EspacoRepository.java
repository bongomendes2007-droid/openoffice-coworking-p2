package br.com.openoffice.repository;

import br.com.openoffice.model.espaco.Espaco;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EspacoRepository extends RepositorioJson<Espaco> {

    public EspacoRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "espacos.json", Espaco.class, "E");
    }

    public List<Espaco> listarAtivos() {
        return filtrar(Espaco::isAtivo).stream()
                .sorted(Comparator.comparing(Espaco::getTipo).thenComparing(Espaco::getNome))
                .toList();
    }

    public boolean existeNome(String nome, String ignorarId) {
        return !filtrar(e -> e.getNome().equalsIgnoreCase(nome.trim()) && !e.getId().equals(ignorarId)).isEmpty();
    }
}
