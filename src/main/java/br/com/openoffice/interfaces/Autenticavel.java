package br.com.openoffice.interfaces;

import br.com.openoffice.enums.Perfil;

/** Contrato de quem pode fazer login no sistema. */
public interface Autenticavel {

    String getLogin();

    String getSenhaHash();

    boolean isAtivo();

    Perfil getPerfil();

    /** Endereço da primeira página depois do login. Cada perfil tem a sua. */
    String getPaginaInicial();
}
