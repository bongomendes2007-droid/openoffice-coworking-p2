package br.com.openoffice.enums;

import br.com.openoffice.model.espaco.Auditorio;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.espaco.EstacaoTrabalho;
import br.com.openoffice.model.espaco.SalaPrivada;
import br.com.openoffice.model.espaco.SalaReuniao;

/**
 * Tipos de espaço. Cada constante sabe criar o objeto concreto correspondente
 * (padrão Factory Method), então o cadastro não precisa de "switch" por tipo.
 */
public enum TipoEspaco {
    ESTACAO("Estação de trabalho") {
        @Override
        public Espaco criar(String nome, int capacidade, String descricao) {
            return new EstacaoTrabalho(nome, descricao);
        }
    },
    SALA_REUNIAO("Sala de reunião") {
        @Override
        public Espaco criar(String nome, int capacidade, String descricao) {
            return new SalaReuniao(nome, capacidade, descricao);
        }
    },
    SALA_PRIVADA("Sala privada") {
        @Override
        public Espaco criar(String nome, int capacidade, String descricao) {
            return new SalaPrivada(nome, capacidade, descricao);
        }
    },
    AUDITORIO("Auditório") {
        @Override
        public Espaco criar(String nome, int capacidade, String descricao) {
            return new Auditorio(nome, capacidade, descricao);
        }
    };

    private final String descricao;

    TipoEspaco(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public abstract Espaco criar(String nome, int capacidade, String descricao);
}
