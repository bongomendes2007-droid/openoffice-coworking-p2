package br.com.openoffice.enums;

import java.math.BigDecimal;
import java.util.List;

/**
 * Planos de assinatura do coworking.
 * Cada constante guarda o próprio preço, a duração e as regras que dependem do plano,
 * evitando "if/else" espalhados pelo código.
 */
public enum TipoPlano {
    DIARIO("Diário", "50.00", 1, false),
    SEMANAL("Semanal", "200.00", 7, false),
    MENSAL("Mensal", "600.00", 30, false),
    VIP("VIP", "900.00", 30, true);

    /** Antecedência máxima para reservas de quem não é VIP (em dias). */
    public static final int ANTECEDENCIA_PADRAO_DIAS = 7;
    /** Antecedência máxima para reservas de membros VIP (em dias). */
    public static final int ANTECEDENCIA_VIP_DIAS = 30;

    private final String descricao;
    private final BigDecimal preco;
    private final int duracaoDias;
    private final boolean vip;

    TipoPlano(String descricao, String preco, int duracaoDias, boolean vip) {
        this.descricao = descricao;
        this.preco = new BigDecimal(preco);
        this.duracaoDias = duracaoDias;
        this.vip = vip;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getDuracaoDias() {
        return duracaoDias;
    }

    public boolean isVip() {
        return vip;
    }

    /** Quantos dias à frente um membro deste plano pode reservar. */
    public int getDiasAntecedenciaReserva() {
        return vip ? ANTECEDENCIA_VIP_DIAS : ANTECEDENCIA_PADRAO_DIAS;
    }

    /** Texto do período cobrado, usado nas telas ("por dia", "por mês"...). */
    public String getPeriodo() {
        return switch (this) {
            case DIARIO -> "por dia";
            case SEMANAL -> "por semana";
            case MENSAL, VIP -> "por mês";
        };
    }

    public List<String> getBeneficios() {
        List<String> base = List.of(
                "Acesso às estações de trabalho",
                "Reserva de salas de reunião e auditório",
                "Até 3 visitantes ao mesmo tempo",
                "Reservas com até " + getDiasAntecedenciaReserva() + " dias de antecedência");
        if (!vip) {
            return base;
        }
        return List.of(
                "Acesso às estações de trabalho",
                "Acesso exclusivo às salas privadas",
                "20% de desconto em salas de reunião e auditório",
                "15% de desconto em eventos especiais",
                "Inscrição antecipada em eventos",
                "Reservas com até " + ANTECEDENCIA_VIP_DIAS + " dias de antecedência",
                "Até 3 visitantes ao mesmo tempo");
    }
}
