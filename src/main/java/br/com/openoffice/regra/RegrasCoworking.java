package br.com.openoffice.regra;

import java.time.LocalTime;

/** Constantes de funcionamento do coworking, reunidas num só lugar. */
public final class RegrasCoworking {

    /** Horário de abertura para reservas. */
    public static final LocalTime ABERTURA = LocalTime.of(8, 0);
    /** Horário de fechamento (as reservas precisam terminar até aqui). */
    public static final LocalTime FECHAMENTO = LocalTime.of(22, 0);
    /** As reservas são feitas em blocos deste tamanho (minutos). */
    public static final int INTERVALO_MINUTOS = 30;
    /** Máximo de visitantes ao mesmo tempo por membro. */
    public static final int MAX_VISITANTES_SIMULTANEOS = 3;
    /** Desconto do VIP em eventos especiais. */
    public static final int DESCONTO_VIP_EVENTO_ESPECIAL = 15;

    private RegrasCoworking() {
    }

    /** Horas de funcionamento por dia (usado no cálculo da taxa de ocupação). */
    public static int horasPorDia() {
        return FECHAMENTO.getHour() - ABERTURA.getHour();
    }
}
