package br.com.openoffice.service;

import br.com.openoffice.model.Notificacao;
import br.com.openoffice.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Avisos que aparecem no painel do membro. */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;
    private final Clock relogio;

    public NotificacaoService(NotificacaoRepository notificacoes, Clock relogio) {
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    public void notificar(String membroId, String mensagem) {
        notificacoes.salvar(new Notificacao(membroId, mensagem, LocalDateTime.now(relogio)));
    }

    public List<Notificacao> recentes(String membroId, int quantidade) {
        return notificacoes.doMembro(membroId).stream().limit(quantidade).toList();
    }

    public long contarNaoLidas(String membroId) {
        return notificacoes.doMembro(membroId).stream().filter(n -> !n.isLida()).count();
    }

    public void marcarTodasComoLidas(String membroId) {
        for (Notificacao n : notificacoes.doMembro(membroId)) {
            if (!n.isLida()) {
                n.marcarComoLida();
                notificacoes.salvar(n);
            }
        }
    }
}
