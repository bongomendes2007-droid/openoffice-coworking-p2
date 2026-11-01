package br.com.openoffice.web.seguranca;

import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Guarda na sessão HTTP apenas o id do usuário logado. A cada requisição o usuário é
 * buscado de novo no repositório, então mudanças (ex.: plano renovado) aparecem na hora.
 */
@Component
public class Sessao {

    static final String CHAVE_USUARIO = "usuarioId";

    private final UsuarioRepository usuarios;

    public Sessao(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public void entrar(HttpSession sessao, Usuario usuario) {
        sessao.setAttribute(CHAVE_USUARIO, usuario.getId());
    }

    public void sair(HttpSession sessao) {
        sessao.invalidate();
    }

    public Optional<Usuario> usuario(HttpSession sessao) {
        Object id = sessao == null ? null : sessao.getAttribute(CHAVE_USUARIO);
        return id == null ? Optional.empty() : usuarios.buscarPorId(id.toString()).filter(Usuario::isAtivo);
    }

    /** Membro logado. O interceptor já garante o perfil, então a exceção é só uma proteção extra. */
    public Membro membro(HttpSession sessao) {
        return usuario(sessao)
                .filter(Membro.class::isInstance)
                .map(Membro.class::cast)
                .orElseThrow(() -> new AcessoNegadoException("Esta página é exclusiva para membros."));
    }
}
