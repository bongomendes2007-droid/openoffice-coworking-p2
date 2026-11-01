package br.com.openoffice.web.form;

import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.model.financeiro.DadosPagamento;

/** Formulário de pagamento (plano, inscrição em evento ou cobrança pendente). */
public class PagamentoForm {

    private TipoPlano plano;
    private DadosPagamento pagamento = new DadosPagamento();

    public TipoPlano getPlano() {
        return plano;
    }

    public void setPlano(TipoPlano plano) {
        this.plano = plano;
    }

    public DadosPagamento getPagamento() {
        return pagamento;
    }

    public void setPagamento(DadosPagamento pagamento) {
        this.pagamento = pagamento;
    }
}
