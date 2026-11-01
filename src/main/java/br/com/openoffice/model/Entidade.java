package br.com.openoffice.model;

import java.util.Objects;

/**
 * Classe base de tudo o que é salvo em arquivo. Garante que cada objeto tenha um id único,
 * gerado pelo repositório no primeiro salvamento.
 */
public abstract class Entidade {

    private String id;

    public String getId() {
        return id;
    }

    /** O id só pode ser definido uma vez (pelo repositório). */
    public void definirId(String id) {
        if (this.id != null) {
            throw new IllegalStateException("O id de uma entidade não pode ser alterado.");
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || id == null) {
            return false;
        }
        return id.equals(((Entidade) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
