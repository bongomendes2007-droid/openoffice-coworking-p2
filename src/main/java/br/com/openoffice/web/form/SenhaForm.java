package br.com.openoffice.web.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SenhaForm {

    @NotBlank(message = "Informe a senha atual.")
    private String senhaAtual;

    @Size(min = 6, message = "A nova senha precisa ter pelo menos 6 caracteres.")
    private String novaSenha;

    private String confirmacao;

    @AssertTrue(message = "As senhas não conferem.")
    public boolean isSenhasIguais() {
        return novaSenha == null || novaSenha.equals(confirmacao);
    }

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacao() {
        return confirmacao;
    }

    public void setConfirmacao(String confirmacao) {
        this.confirmacao = confirmacao;
    }
}
