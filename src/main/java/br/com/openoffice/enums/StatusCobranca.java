package br.com.openoffice.enums;

public enum StatusCobranca {
    PENDENTE("Pendente"),
    PAGA("Paga"),
    ESTORNADA("Estornada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
