package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.FormaPagamento;

import java.time.LocalDateTime;
import java.util.UUID;

/** Pagamento instantâneo via PIX (simulado: é aprovado na hora). */
public class PagamentoPix extends Pagamento {

    public PagamentoPix() {
    }

    @Override
    public FormaPagamento getForma() {
        return FormaPagamento.PIX;
    }

    @Override
    protected void validar(LocalDateTime agora) {
        // PIX não exige dados extras do membro.
    }

    @Override
    protected String gerarComprovante() {
        return "PIX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String getDetalhe() {
        return "PIX";
    }
}
