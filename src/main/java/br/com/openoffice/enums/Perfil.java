package br.com.openoffice.enums;

/** Perfis de acesso do sistema. Cada perfil enxerga um conjunto de páginas. */
public enum Perfil {
    ADMIN("Administrador"),
    MEMBRO("Membro");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
