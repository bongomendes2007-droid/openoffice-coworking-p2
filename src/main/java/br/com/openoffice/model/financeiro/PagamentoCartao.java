package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.FormaPagamento;
import br.com.openoffice.exception.PagamentoRecusadoException;
import br.com.openoffice.util.Validador;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pagamento com cartão de crédito (simulado). Valida número (Luhn), validade, CVV e parcelas.
 * Os campos "transient" não são gravados no arquivo: só o final do cartão fica salvo.
 */
public class PagamentoCartao extends Pagamento {

    public static final int MAX_PARCELAS = 6;

    private String titular;
    private String finalCartao;
    private int parcelas;
    private transient String numero;
    private transient String validade;
    private transient String cvv;

    protected PagamentoCartao() {
    }

    public PagamentoCartao(String titular, String numero, String validade, String cvv, int parcelas) {
        this.titular = titular == null ? "" : titular.trim();
        this.numero = Validador.apenasDigitos(numero);
        this.validade = validade == null ? "" : validade.trim();
        this.cvv = Validador.apenasDigitos(cvv);
        this.parcelas = parcelas;
    }

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.CARTAO;
    }

    @Override
    protected void validar(LocalDateTime agora) {
        if (titular.isBlank()) {
            throw new PagamentoRecusadoException("Informe o nome impresso no cartão.");
        }
        if (!Validador.cartaoValido(numero)) {
            throw new PagamentoRecusadoException("Número de cartão inválido. Confira os dígitos.");
        }
        YearMonth vencimento;
        try {
            vencimento = YearMonth.parse(validade, DateTimeFormatter.ofPattern("MM/yy"));
        } catch (DateTimeParseException e) {
            throw new PagamentoRecusadoException("Validade inválida. Use o formato MM/AA, por exemplo 08/28.");
        }
        if (vencimento.isBefore(YearMonth.from(agora))) {
            throw new PagamentoRecusadoException("Cartão vencido em " + validade + ".");
        }
        if (cvv.length() < 3 || cvv.length() > 4) {
            throw new PagamentoRecusadoException("O código de segurança (CVV) deve ter 3 ou 4 dígitos.");
        }
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new PagamentoRecusadoException("Escolha de 1 a " + MAX_PARCELAS + " parcelas.");
        }
        finalCartao = numero.substring(numero.length() - 4);
    }

    @Override
    protected String gerarComprovante() {
        return "AUT-" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    @Override
    public String getDetalhe() {
        return "Cartão final " + finalCartao + (parcelas > 1 ? " em " + parcelas + "x" : " à vista");
    }

    public String getTitular() {
        return titular;
    }

    public String getFinalCartao() {
        return finalCartao;
    }

    public int getParcelas() {
        return parcelas;
    }
}
