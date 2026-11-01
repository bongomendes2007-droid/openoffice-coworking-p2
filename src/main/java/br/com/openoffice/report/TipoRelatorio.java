package br.com.openoffice.report;

import br.com.openoffice.exception.RecursoNaoEncontradoException;

import java.util.Arrays;

/** Relatórios disponíveis para o administrador. */
public enum TipoRelatorio {
    OCUPACAO_SALA("ocupacao-sala", "Ocupação por sala", "bi-door-open"),
    OCUPACAO_HORARIO("ocupacao-horario", "Ocupação por horário", "bi-clock"),
    FINANCEIRO("financeiro", "Financeiro", "bi-cash-coin"),
    VISITAS("visitas", "Visitas", "bi-person-badge"),
    EVENTOS("eventos", "Eventos", "bi-calendar-event");

    private final String codigo;
    private final String descricao;
    private final String icone;

    TipoRelatorio(String codigo, String descricao, String icone) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.icone = icone;
    }

    public static TipoRelatorio porCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(t -> t.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório \"" + codigo + "\" não existe."));
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getIcone() {
        return icone;
    }
}
