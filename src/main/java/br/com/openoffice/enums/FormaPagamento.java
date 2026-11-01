package br.com.openoffice.enums;

import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.financeiro.Pagamento;
import br.com.openoffice.model.financeiro.PagamentoBoleto;
import br.com.openoffice.model.financeiro.PagamentoCartao;
import br.com.openoffice.model.financeiro.PagamentoPix;

/** Formas de pagamento aceitas. Cada uma cria a sua subclasse de {@link Pagamento}. */
public enum FormaPagamento {
    PIX("PIX") {
        @Override
        public Pagamento criarPagamento(DadosPagamento dados) {
            return new PagamentoPix();
        }
    },
    CARTAO("Cartão de crédito") {
        @Override
        public Pagamento criarPagamento(DadosPagamento dados) {
            return new PagamentoCartao(dados.getTitular(), dados.getNumeroCartao(),
                    dados.getValidade(), dados.getCvv(), dados.getParcelas());
        }
    },
    BOLETO("Boleto") {
        @Override
        public Pagamento criarPagamento(DadosPagamento dados) {
            return new PagamentoBoleto();
        }
    };

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public abstract Pagamento criarPagamento(DadosPagamento dados);
}
