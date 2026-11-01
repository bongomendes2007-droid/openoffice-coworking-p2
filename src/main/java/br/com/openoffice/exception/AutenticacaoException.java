package br.com.openoffice.exception;

/** Falha no login (e-mail ou senha incorretos, conta bloqueada ou desativada). */
public class AutenticacaoException extends RegraNegocioException {

    public AutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
