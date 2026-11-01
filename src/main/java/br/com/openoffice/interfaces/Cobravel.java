package br.com.openoffice.interfaces;

import br.com.openoffice.enums.TipoCobranca;

import java.math.BigDecimal;

/**
 * Tudo o que gera cobrança para o membro: assinatura de plano, reserva e inscrição em evento.
 * O serviço financeiro trabalha só com esta interface, sem conhecer as classes concretas.
 */
public interface Cobravel {

    TipoCobranca getTipoCobranca();

    String getDescricaoCobranca();

    BigDecimal getValorCobrado();

    /** Identificador do item cobrado (ex.: id da reserva ou do evento). */
    String getReferenciaCobranca();
}
