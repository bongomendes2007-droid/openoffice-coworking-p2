package br.com.openoffice.web.form;

import br.com.openoffice.service.dto.NovoEvento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class EventoForm {

    @NotBlank(message = "Informe o título do evento.")
    @Size(max = 80, message = "O título pode ter no máximo 80 caracteres.")
    private String titulo;

    @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
    private String descricao;

    @NotBlank(message = "Escolha o espaço.")
    private String espacoId;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horário de início.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime inicio;

    @NotNull(message = "Informe o horário de fim.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime fim;

    @NotNull(message = "Informe o número de vagas.")
    @Min(value = 1, message = "O evento precisa ter pelo menos 1 vaga.")
    private Integer capacidade = 20;

    @NotNull(message = "Informe o preço (0 para evento gratuito).")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    @DecimalMax(value = "10000.00", message = "Preço muito alto.")
    private BigDecimal preco = BigDecimal.ZERO;

    private boolean especial;

    @NotNull(message = "Informe os dias de prioridade VIP.")
    @Min(value = 0, message = "Use de 0 a 30 dias.")
    @Max(value = 30, message = "Use de 0 a 30 dias.")
    private Integer diasPrioridadeVip = 2;

    public NovoEvento paraNovoEvento() {
        return new NovoEvento(titulo, descricao, espacoId, data.atTime(inicio), data.atTime(fim), capacidade, preco,
                especial, diasPrioridadeVip);
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getEspacoId() {
        return espacoId;
    }

    public void setEspacoId(String espacoId) {
        this.espacoId = espacoId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalTime inicio) {
        this.inicio = inicio;
    }

    public LocalTime getFim() {
        return fim;
    }

    public void setFim(LocalTime fim) {
        this.fim = fim;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public boolean isEspecial() {
        return especial;
    }

    public void setEspecial(boolean especial) {
        this.especial = especial;
    }

    public Integer getDiasPrioridadeVip() {
        return diasPrioridadeVip;
    }

    public void setDiasPrioridadeVip(Integer diasPrioridadeVip) {
        this.diasPrioridadeVip = diasPrioridadeVip;
    }
}
