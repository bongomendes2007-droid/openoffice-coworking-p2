package br.com.openoffice.model.pessoa;

import br.com.openoffice.model.Entidade;
import br.com.openoffice.util.Validador;

/** Abstração de qualquer pessoa que circula no coworking (membros, administradores e visitantes). */
public abstract class Pessoa extends Entidade {

    private String nome;
    private String documento;

    protected Pessoa() {
    }

    protected Pessoa(String nome, String documento) {
        setNome(nome);
        setDocumento(documento);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validador.obrigatorio(nome, "nome");
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento == null ? null : documento.trim();
    }

    /** Primeiro nome, usado nas saudações. */
    public String getPrimeiroNome() {
        return nome == null ? "" : nome.split("\\s+")[0];
    }
}
