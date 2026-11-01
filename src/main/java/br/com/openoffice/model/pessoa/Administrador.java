package br.com.openoffice.model.pessoa;

import br.com.openoffice.enums.Perfil;

import java.time.LocalDateTime;

/** Funcionário da recepção/gestão. Cadastra espaços e eventos e emite relatórios. */
public class Administrador extends Usuario {

    protected Administrador() {
    }

    public Administrador(String nome, String email, String senhaHash, LocalDateTime criadoEm) {
        super(nome, null, email, senhaHash, criadoEm);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.ADMIN;
    }

    @Override
    public String getPaginaInicial() {
        return "/admin/painel";
    }
}
