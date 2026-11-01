package br.com.openoffice.repository;

import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.util.Validador;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepository extends RepositorioJson<Usuario> {

    public UsuarioRepository(ArmazenamentoJson armazenamento) {
        super(armazenamento, "usuarios.json", Usuario.class, "U");
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String alvo = email.trim().toLowerCase();
        return filtrar(u -> u.getEmail().equals(alvo)).stream().findFirst();
    }

    public Optional<Membro> buscarMembro(String id) {
        return buscarPorId(id).filter(Membro.class::isInstance).map(Membro.class::cast);
    }

    public List<Membro> listarMembros() {
        return listarTodos().stream()
                .filter(Membro.class::isInstance)
                .map(Membro.class::cast)
                .sorted(Comparator.comparing(Membro::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean existeCpf(String cpf, String ignorarId) {
        String alvo = Validador.apenasDigitos(cpf);
        return !filtrar(u -> u.getDocumento() != null
                && Validador.apenasDigitos(u.getDocumento()).equals(alvo)
                && !u.getId().equals(ignorarId)).isEmpty();
    }

    public boolean existeEmail(String email, String ignorarId) {
        return buscarPorEmail(email).filter(u -> !u.getId().equals(ignorarId)).isPresent();
    }
}
