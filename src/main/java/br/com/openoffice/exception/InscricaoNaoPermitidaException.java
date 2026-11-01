package br.com.openoffice.exception;

/** A inscrição no evento não é permitida neste momento. */
public class InscricaoNaoPermitidaException extends RegraNegocioException {

    public InscricaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
