package br.com.openoffice.model.espaco;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;

/** Mesa individual. O uso já está incluído em todos os planos, por isso não tem preço por hora. */
public class EstacaoTrabalho extends Espaco {

    protected EstacaoTrabalho() {
    }

    public EstacaoTrabalho(String nome, String descricao) {
        super(nome, 1, descricao);
    }

    @Override
    public TipoEspaco getTipo() {
        return TipoEspaco.ESTACAO;
    }

    @Override
    public BigDecimal getPrecoHora() {
        return Dinheiro.ZERO;
    }

    /** Uma estação comporta sempre uma pessoa, mesmo que alguém tente alterar. */
    @Override
    public void setCapacidade(int capacidade) {
        super.setCapacidade(1);
    }
}
