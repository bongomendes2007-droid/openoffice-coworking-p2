package br.com.openoffice.exception;

/** A data da reserva passa do limite de antecedência do plano. */
public class AntecedenciaInvalidaException extends RegraNegocioException {

    public AntecedenciaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
