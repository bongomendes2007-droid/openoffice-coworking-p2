package br.com.openoffice.web.erro;

import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.service.NotificacaoService;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Coloca em todas as páginas os dados usados pelo menu (usuário logado, avisos, data atual). */
@ControllerAdvice
public class ModeloGlobal {

    private final Sessao sessao;
    private final NotificacaoService notificacoes;
    private final Clock relogio;

    public ModeloGlobal(Sessao sessao, NotificacaoService notificacoes, Clock relogio) {
        this.sessao = sessao;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario usuarioLogado(HttpServletRequest request) {
        return sessao.usuario(request.getSession(false)).orElse(null);
    }

    @ModelAttribute("avisosNaoLidos")
    public long avisosNaoLidos(HttpServletRequest request) {
        return sessao.usuario(request.getSession(false))
                .filter(Membro.class::isInstance)
                .map(u -> notificacoes.contarNaoLidas(u.getId()))
                .orElse(0L);
    }

    @ModelAttribute("agora")
    public LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    @ModelAttribute("hoje")
    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @ModelAttribute("paginaAtual")
    public String paginaAtual(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
