package br.com.openoffice.repository;

import br.com.openoffice.model.Entidade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Implementação genérica de {@link Repositorio} que mantém os objetos em memória
 * e grava o arquivo JSON a cada alteração. Cada entidade concreta tem uma subclasse
 * pequena que só informa o nome do arquivo e o prefixo do id (ex.: "R" para reservas).
 *
 * @param <T> tipo da entidade guardada
 */
public abstract class RepositorioJson<T extends Entidade> implements Repositorio<T> {

    private final ArmazenamentoJson armazenamento;
    private final String arquivo;
    private final Class<T> tipo;
    private final String prefixoId;
    private final Map<String, T> dados = new LinkedHashMap<>();

    protected RepositorioJson(ArmazenamentoJson armazenamento, String arquivo, Class<T> tipo, String prefixoId) {
        this.armazenamento = armazenamento;
        this.arquivo = arquivo;
        this.tipo = tipo;
        this.prefixoId = prefixoId;
        recarregar();
    }

    /** Lê novamente o arquivo (usado depois de baixar os dados do Google Drive). */
    public final synchronized void recarregar() {
        dados.clear();
        for (T item : armazenamento.ler(arquivo, tipo)) {
            if (item != null && item.getId() != null) {
                dados.put(item.getId(), item);
            }
        }
    }

    @Override
    public synchronized <S extends T> S salvar(S entidade) {
        if (entidade.getId() == null) {
            entidade.definirId(gerarId());
        }
        dados.put(entidade.getId(), entidade);
        persistir();
        return entidade;
    }

    @Override
    public synchronized Optional<T> buscarPorId(String id) {
        return Optional.ofNullable(id == null ? null : dados.get(id));
    }

    @Override
    public synchronized List<T> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public synchronized List<T> filtrar(Predicate<T> condicao) {
        return dados.values().stream().filter(condicao).toList();
    }

    @Override
    public synchronized void remover(String id) {
        if (dados.remove(id) != null) {
            persistir();
        }
    }

    @Override
    public synchronized int contar() {
        return dados.size();
    }

    public String getArquivo() {
        return arquivo;
    }

    /** Gera ids legíveis e sequenciais: R0001, R0002... */
    private String gerarId() {
        int maior = dados.keySet().stream()
                .filter(id -> id.startsWith(prefixoId))
                .map(id -> id.substring(prefixoId.length()))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return prefixoId + String.format("%04d", maior + 1);
    }

    private void persistir() {
        armazenamento.gravar(arquivo, new ArrayList<>(dados.values()), tipo);
    }
}
