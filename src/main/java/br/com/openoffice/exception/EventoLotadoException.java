package br.com.openoffice.exception;

/** Todas as vagas do evento estão ocupadas. */
public class EventoLotadoException extends RegraNegocioException {

    public EventoLotadoException(String mensagem) {
        super(mensagem);
    }
}
