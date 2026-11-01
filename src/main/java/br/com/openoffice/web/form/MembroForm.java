package br.com.openoffice.web.form;

import br.com.openoffice.model.pessoa.Membro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição de membro pelo administrador (sem senha). */
public class MembroForm {

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

    public static MembroForm de(Membro m) {
        MembroForm f = new MembroForm();
        f.nome = m.getNome();
        f.cpf = m.getDocumento();
        f.email = m.getEmail();
        f.telefone = m.getTelefone();
        return f;
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
}
