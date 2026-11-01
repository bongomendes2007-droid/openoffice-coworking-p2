package br.com.openoffice.web.form;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.model.espaco.Espaco;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EspacoForm {

    @NotNull(message = "Escolha o tipo do espaço.")
    private TipoEspaco tipo;

    @NotBlank(message = "Informe o nome do espaço.")
    @Size(max = 60, message = "O nome pode ter no máximo 60 caracteres.")
    private String nome;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade mínima é 1 pessoa.")
    @Max(value = 1000, message = "A capacidade máxima é 1000 pessoas.")
    private Integer capacidade = 1;

    @Size(max = 300, message = "A descrição pode ter no máximo 300 caracteres.")
    private String descricao;

    public static EspacoForm de(Espaco e) {
        EspacoForm f = new EspacoForm();
        f.tipo = e.getTipo();
        f.nome = e.getNome();
        f.capacidade = e.getCapacidade();
        f.descricao = e.getDescricao();
        return f;
    }

    public TipoEspaco getTipo() {
        return tipo;
    }

    public void setTipo(TipoEspaco tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
