package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.FormaPagamento;
import br.com.openoffice.model.Entidade;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transação que quita uma cobrança. Cada forma de pagamento é uma subclasse com a sua
 * própria validação e o seu próprio comprovante.
 *
 * O método {@link #processar} é um Template Method: o passo a passo é sempre o mesmo
 * (validar, gerar comprovante, registrar data) e as subclasses preenchem os detalhes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "forma")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PagamentoPix.class, name = "PIX"),
        @JsonSubTypes.Type(value = PagamentoCartao.class, name = "CARTAO"),
        @JsonSubTypes.Type(value = PagamentoBoleto.class, name = "BOLETO")
})
public abstract class Pagamento extends Entidade {

    private String cobrancaId;
    private String membroId;
    private BigDecimal valor;
    private LocalDateTime realizadoEm;
    private String comprovante;

    protected Pagamento() {
    }

    public abstract FormaPagamento getForma();

    /** Confere os dados específicos da forma de pagamento. Lança PagamentoRecusadoException se inválidos. */
    protected abstract void validar(LocalDateTime agora);

    /** Gera o código que identifica o pagamento (txid do PIX, autorização do cartão, linha do boleto). */
    protected abstract String gerarComprovante();

    /** Descrição curta para o histórico (ex.: "Cartão final 4242 em 3x"). */
    public abstract String getDetalhe();

    public final void processar(String membroId, BigDecimal valor, LocalDateTime agora) {
        validar(agora);
        this.membroId = membroId;
        this.valor = valor;
        this.comprovante = gerarComprovante();
        this.realizadoEm = agora;
    }

    /** Confere os dados sem efetuar o pagamento (usado antes de gravar a reserva ou a inscrição). */
    public final void conferirDados(LocalDateTime agora) {
        validar(agora);
    }

    public void vincularCobranca(String cobrancaId) {
        this.cobrancaId = cobrancaId;
    }

    public String getCobrancaId() {
        return cobrancaId;
    }

    public String getMembroId() {
        return membroId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getRealizadoEm() {
        return realizadoEm;
    }

    public String getComprovante() {
        return comprovante;
    }
}
