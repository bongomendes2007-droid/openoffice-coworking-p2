package br.com.openoffice.exception;

/** O espaço já está reservado no horário pedido. */
public class HorarioIndisponivelException extends RegraNegocioException {

    public HorarioIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
