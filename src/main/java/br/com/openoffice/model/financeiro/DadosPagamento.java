package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.FormaPagamento;

/**
 * Dados digitados pelo membro na tela de pagamento.
 * Os dados do cartão nunca são gravados em arquivo: só os 4 últimos dígitos ficam no comprovante.
 */
public class DadosPagamento {

    private FormaPagamento forma = FormaPagamento.PIX;
    private String titular;
    private String numeroCartao;
    private String validade;
    private String cvv;
    private int parcelas = 1;

    public DadosPagamento() {
    }

    public static DadosPagamento pix() {
        return new DadosPagamento();
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public void setForma(FormaPagamento forma) {
        this.forma = forma;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getNumeroCartao() {
        return numeroCartao;
    }

    public void setNumeroCartao(String numeroCartao) {
        this.numeroCartao = numeroCartao;
    }

    public String getValidade() {
        return validade;
    }

    public void setValidade(String validade) {
        this.validade = validade;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public int getParcelas() {
        return parcelas;
    }

    public void setParcelas(int parcelas) {
        this.parcelas = parcelas;
    }
}
