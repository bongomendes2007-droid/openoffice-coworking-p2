package br.com.openoffice.enums;

public enum StatusInscricao {
    CONFIRMADA("Confirmada", true),
    AGUARDANDO_PAGAMENTO("Aguardando pagamento", true),
    CANCELADA("Cancelada", false);

    private final String descricao;
    private final boolean ocupaVaga;

    StatusInscricao(String descricao, boolean ocupaVaga) {
        this.descricao = descricao;
        this.ocupaVaga = ocupaVaga;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Indica se a inscrição conta como vaga ocupada no evento. */
    public boolean isOcupaVaga() {
        return ocupaVaga;
    }
}
