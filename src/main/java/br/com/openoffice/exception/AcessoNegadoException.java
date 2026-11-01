package br.com.openoffice.exception;

/** O usuário não tem permissão para usar o recurso (ex.: sala privada sem plano VIP). */
public class AcessoNegadoException extends RegraNegocioException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
