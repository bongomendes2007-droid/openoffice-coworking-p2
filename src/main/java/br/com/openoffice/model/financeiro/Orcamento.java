package br.com.openoffice.model.financeiro;

import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;

/**
 * Resultado de um cálculo de preço: valor base, desconto aplicado e total.
 * É mostrado ao membro antes de confirmar uma reserva ou inscrição.
 */
public record Orcamento(BigDecimal valorBase, int percentualDesconto, BigDecimal desconto, BigDecimal total) {

    public static Orcamento de(BigDecimal valorBase, int percentualDesconto) {
        BigDecimal base = Dinheiro.arredondar(valorBase);
        BigDecimal desconto = Dinheiro.percentual(base, percentualDesconto);
        return new Orcamento(base, percentualDesconto, desconto, base.subtract(desconto));
    }

    public boolean temDesconto() {
        return percentualDesconto > 0;
    }

    public boolean gratuito() {
        return total.signum() == 0;
    }
}
