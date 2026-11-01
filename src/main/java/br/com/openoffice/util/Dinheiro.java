package br.com.openoffice.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Operações com valores em reais (sempre 2 casas decimais). */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal CEM = new BigDecimal("100");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Calcula "percentual"% de "valor". Ex.: percentual(120, 20) = 24,00. */
    public static BigDecimal percentual(BigDecimal valor, int percentual) {
        return arredondar(valor.multiply(BigDecimal.valueOf(percentual)).divide(CEM, 4, RoundingMode.HALF_UP));
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
