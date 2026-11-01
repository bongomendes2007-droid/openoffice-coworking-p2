package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.FormaPagamento;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/** Pagamento por boleto (simulado: a compensação é considerada imediata). */
public class PagamentoBoleto extends Pagamento {

    private String linhaDigitavel;

    public PagamentoBoleto() {
    }

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.BOLETO;
    }

    @Override
    protected void validar(LocalDateTime agora) {
        // O boleto é gerado pelo próprio sistema, não há dados a validar.
    }

    @Override
    protected String gerarComprovante() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        linhaDigitavel = String.format("34191.%05d %05d.%06d %05d.%06d 1 %014d",
                r.nextInt(100000), r.nextInt(100000), r.nextInt(1000000), r.nextInt(100000),
                r.nextInt(1000000), r.nextLong(100000000000000L));
        return "BOL-" + linhaDigitavel.substring(6, 11);
    }

    @Override
    public String getDetalhe() {
        return "Boleto " + linhaDigitavel;
    }

    public String getLinhaDigitavel() {
        return linhaDigitavel;
    }
}
