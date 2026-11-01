package br.com.openoffice.web.seguranca;

import br.com.openoffice.enums.Perfil;
import br.com.openoffice.model.pessoa.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Executado antes de cada página protegida:
 * - sem login: redireciona para /login;
 * - perfil errado (membro tentando abrir /admin, por exemplo): mostra a página 403.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    private final Sessao sessao;

    public AutenticacaoInterceptor(Sessao sessao) {
        this.sessao = sessao;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Optional<Usuario> usuario = sessao.usuario(request.getSession(false));
        if (usuario.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login?expirou");
            return false;
        }
        Perfil exigido = request.getRequestURI().startsWith(request.getContextPath() + "/admin")
                ? Perfil.ADMIN : Perfil.MEMBRO;
        if (usuario.get().getPerfil() != exigido) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
