package br.com.openoffice.service.dto;

import java.math.BigDecimal;

/** Valores envolvidos num cancelamento, mostrados antes e depois da confirmação. */
public record ResultadoCancelamento(String descricao, BigDecimal valorPago, BigDecimal multa,
                                    BigDecimal reembolso, long horasAteInicio, boolean cobraMulta) {
}
