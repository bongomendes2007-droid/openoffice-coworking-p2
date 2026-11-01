package br.com.openoffice.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Contrato genérico de armazenamento. Os serviços dependem desta interface,
 * não do formato do arquivo; trocar JSON por banco de dados não mudaria as regras.
 *
 * @param <T> tipo da entidade guardada
 */
public interface Repositorio<T> {

    <S extends T> S salvar(S entidade);

    Optional<T> buscarPorId(String id);

    List<T> listarTodos();

    List<T> filtrar(Predicate<T> condicao);

    void remover(String id);

    int contar();
}
