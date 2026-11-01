package br.com.openoffice.exception;

/**
 * Exceção base para qualquer violação de regra do coworking.
 * As telas capturam este tipo e mostram a mensagem ao usuário sem interromper o sistema.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
