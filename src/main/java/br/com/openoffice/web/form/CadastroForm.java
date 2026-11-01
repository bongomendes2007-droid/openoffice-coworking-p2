package br.com.openoffice.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulário de cadastro de membro (página pública e área do administrador). */
public class CadastroForm {

    @NotBlank(message = "Informe o nome completo.")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o CPF.")
    private String cpf;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "E-mail inválido. Exemplo: nome@dominio.com")
    private String email;

    @Size(max = 20, message = "Telefone muito longo.")
    private String telefone;

    @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.")
    private String senha;

    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas não conferem.")
    public boolean isSenhasIguais() {
        return senha == null || senha.equals(confirmacaoSenha);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
