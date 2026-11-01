package br.com.openoffice.model.espaco;

import br.com.openoffice.enums.TipoEspaco;

import java.math.BigDecimal;

/** Sala de reunião: R$ 60,00/hora, com 20% de desconto para VIP. */
public class SalaReuniao extends Espaco {

    public static final BigDecimal PRECO_HORA = new BigDecimal("60.00");
    public static final int DESCONTO_VIP = 20;

    protected SalaReuniao() {
    }

    public SalaReuniao(String nome, int capacidade, String descricao) {
        super(nome, capacidade, descricao);
    }

    @Override
    public TipoEspaco getTipo() {
        return TipoEspaco.SALA_REUNIAO;
    }

    @Override
    public BigDecimal getPrecoHora() {
        return PRECO_HORA;
    }

    @Override
    protected int percentualDesconto(boolean membroVip) {
        return membroVip ? DESCONTO_VIP : 0;
    }

    @Override
    public boolean aceitaEventos() {
        return true;
    }
}
