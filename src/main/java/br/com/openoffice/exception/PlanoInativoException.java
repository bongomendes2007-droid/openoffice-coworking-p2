package br.com.openoffice.exception;

/** O membro não tem um plano ativo. */
public class PlanoInativoException extends RegraNegocioException {

    public PlanoInativoException(String mensagem) {
        super(mensagem);
    }
}
