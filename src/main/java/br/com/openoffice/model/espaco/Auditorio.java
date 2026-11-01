package br.com.openoffice.model.espaco;

import br.com.openoffice.enums.TipoEspaco;

import java.math.BigDecimal;

/** Auditório: R$ 200,00/hora, com 20% de desconto para VIP. Usado também em eventos. */
public class Auditorio extends Espaco {

    public static final BigDecimal PRECO_HORA = new BigDecimal("200.00");
    public static final int DESCONTO_VIP = 20;

    protected Auditorio() {
    }

    public Auditorio(String nome, int capacidade, String descricao) {
        super(nome, capacidade, descricao);
    }

    @Override
    public TipoEspaco getTipo() {
        return TipoEspaco.AUDITORIO;
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
