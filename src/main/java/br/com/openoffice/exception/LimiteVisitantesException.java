package br.com.openoffice.exception;

/** O membro já tem o número máximo de visitantes ao mesmo tempo. */
public class LimiteVisitantesException extends RegraNegocioException {

    public LimiteVisitantesException(String mensagem) {
        super(mensagem);
    }
}
