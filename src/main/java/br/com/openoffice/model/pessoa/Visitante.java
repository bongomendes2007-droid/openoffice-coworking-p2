package br.com.openoffice.model.pessoa;

import br.com.openoffice.util.Validador;

/** Pessoa sem login que entra no coworking autorizada por um membro. */
public class Visitante extends Pessoa {

    protected Visitante() {
    }

    public Visitante(String nome, String documento) {
        super(nome, Validador.obrigatorio(documento, "documento"));
    }

    /** Documento sem pontuação, usado para encontrar o mesmo visitante em visitas futuras. */
    public String getDocumentoNormalizado() {
        return Validador.normalizarDocumento(getDocumento());
    }
}
