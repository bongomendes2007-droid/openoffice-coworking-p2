package br.com.openoffice.enums;

/** Origem de uma cobrança. Usado no histórico financeiro e nos relatórios. */
public enum TipoCobranca {
    PLANO("Plano"),
    RESERVA("Reserva"),
    EVENTO("Evento"),
    MULTA("Multa de cancelamento");

    private final String descricao;

    TipoCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
