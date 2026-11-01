package br.com.openoffice.model.espaco;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.model.pessoa.Membro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sala privada: R$ 80,00/hora e exclusiva para membros do plano VIP. */
public class SalaPrivada extends Espaco {

    public static final BigDecimal PRECO_HORA = new BigDecimal("80.00");

    protected SalaPrivada() {
    }

    public SalaPrivada(String nome, int capacidade, String descricao) {
        super(nome, capacidade, descricao);
    }

    @Override
    public TipoEspaco getTipo() {
        return TipoEspaco.SALA_PRIVADA;
    }

    @Override
    public BigDecimal getPrecoHora() {
        return PRECO_HORA;
    }

    /** Mantém a regra geral (plano ativo) e acrescenta a exigência de plano VIP. */
    @Override
    public void validarAcesso(Membro membro, LocalDate hoje) {
        super.validarAcesso(membro, hoje);
        if (!membro.isVip(hoje)) {
            throw new AcessoNegadoException("Acesso negado: as salas privadas são exclusivas do plano VIP.");
        }
    }

    @Override
    public boolean isExclusivoVip() {
        return true;
    }
}
