package br.com.openoffice.exception;

/** O número de pessoas é maior que a capacidade do espaço ou do evento. */
public class CapacidadeExcedidaException extends RegraNegocioException {

    public CapacidadeExcedidaException(String mensagem) {
        super(mensagem);
    }
}
