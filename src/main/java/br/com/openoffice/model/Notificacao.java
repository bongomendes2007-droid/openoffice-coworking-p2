package br.com.openoffice.model;

import java.time.LocalDateTime;

/** Aviso exibido no painel do membro (vaga liberada em evento, plano vencendo...). */
public class Notificacao extends Entidade {

    private String membroId;
    private String mensagem;
    private LocalDateTime criadaEm;
    private boolean lida;

    protected Notificacao() {
    }

    public Notificacao(String membroId, String mensagem, LocalDateTime criadaEm) {
        this.membroId = membroId;
        this.mensagem = mensagem;
        this.criadaEm = criadaEm;
    }

    public void marcarComoLida() {
        this.lida = true;
    }

    public String getMembroId() {
        return membroId;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public boolean isLida() {
        return lida;
    }
}
