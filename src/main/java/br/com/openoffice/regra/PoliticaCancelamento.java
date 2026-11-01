package br.com.openoffice.regra;

import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regra de cancelamento usada por reservas e inscrições em eventos:
 * cancelar com menos de 24h de antecedência gera multa de 50% do valor pago.
 */
public final class PoliticaCancelamento {

    public static final long HORAS_MINIMAS_SEM_MULTA = 24;
    public static final int PERCENTUAL_MULTA = 50;

    private PoliticaCancelamento() {
    }

    public static boolean cobraMulta(LocalDateTime inicio, LocalDateTime agora) {
        return Duration.between(agora, inicio).toMinutes() < HORAS_MINIMAS_SEM_MULTA * 60;
    }

    public static BigDecimal calcularMulta(BigDecimal valorPago, LocalDateTime inicio, LocalDateTime agora) {
        if (!cobraMulta(inicio, agora) || !Dinheiro.positivo(valorPago)) {
            return Dinheiro.ZERO;
        }
        return Dinheiro.percentual(valorPago, PERCENTUAL_MULTA);
    }

    /** Horas que faltam para o início (usado nas mensagens de confirmação). */
    public static long horasAteInicio(LocalDateTime inicio, LocalDateTime agora) {
        return Math.max(0, Duration.between(agora, inicio).toHours());
    }
}
