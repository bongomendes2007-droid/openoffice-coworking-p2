package br.com.openoffice.exception;

/** Dados informados inválidos (CPF, e-mail, datas, horários, período...). */
public class DadosInvalidosException extends RegraNegocioException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
