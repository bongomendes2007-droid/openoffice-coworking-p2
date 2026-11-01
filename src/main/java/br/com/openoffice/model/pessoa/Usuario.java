package br.com.openoffice.model.pessoa;

import br.com.openoffice.interfaces.Autenticavel;
import br.com.openoffice.util.Validador;
import br.com.openoffice.exception.DadosInvalidosException;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

/**
 * Pessoa que pode fazer login. Membros e administradores ficam no mesmo arquivo;
 * o campo "perfil" do JSON diz qual subclasse deve ser criada na leitura.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "perfil")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Membro.class, name = "MEMBRO"),
        @JsonSubTypes.Type(value = Administrador.class, name = "ADMIN")
})
public abstract class Usuario extends Pessoa implements Autenticavel {

    private String email;
    private String senhaHash;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    protected Usuario(String nome, String documento, String email, String senhaHash, LocalDateTime criadoEm) {
        super(nome, documento);
        setEmail(email);
        this.senhaHash = senhaHash;
        this.criadoEm = criadoEm;
    }

    @Override
    public String getLogin() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!Validador.emailValido(email)) {
            throw new DadosInvalidosException("E-mail inválido. Exemplo: nome@dominio.com");
        }
        this.email = email.trim().toLowerCase();
    }

    @Override
    public String getSenhaHash() {
        return senhaHash;
    }

    public void alterarSenhaHash(String novoHash) {
        this.senhaHash = novoHash;
    }

    @Override
    public boolean isAtivo() {
        return ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
