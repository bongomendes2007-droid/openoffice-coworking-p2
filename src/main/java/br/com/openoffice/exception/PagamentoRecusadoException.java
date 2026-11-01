package br.com.openoffice.exception;

/** Os dados do pagamento foram recusados. */
public class PagamentoRecusadoException extends RegraNegocioException {

    public PagamentoRecusadoException(String mensagem) {
        super(mensagem);
    }
}
