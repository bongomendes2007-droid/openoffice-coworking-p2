package br.com.openoffice.web.form;

import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.service.dto.SolicitacaoReserva;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Formulário de nova reserva (passo 1: dados; passo 2: pagamento). */
public class ReservaForm {

    @NotBlank(message = "Escolha um espaço.")
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

    @NotNull(message = "Informe o número de participantes.")
    @Min(value = 1, message = "Informe pelo menos 1 participante.")
    @Max(value = 500, message = "Número de participantes muito alto.")
    private Integer participantes = 1;

    private DadosPagamento pagamento = new DadosPagamento();

    public SolicitacaoReserva paraSolicitacao() {
        return new SolicitacaoReserva(espacoId, data, inicio, fim, participantes == null ? 0 : participantes);
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

    public Integer getParticipantes() {
        return participantes;
    }

    public void setParticipantes(Integer participantes) {
        this.participantes = participantes;
    }

    public DadosPagamento getPagamento() {
        return pagamento;
    }

    public void setPagamento(DadosPagamento pagamento) {
        this.pagamento = pagamento;
    }
}
