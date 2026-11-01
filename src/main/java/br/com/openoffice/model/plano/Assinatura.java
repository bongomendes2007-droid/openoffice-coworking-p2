package br.com.openoffice.model.plano;

import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.interfaces.Cobravel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Período em que um membro tem um plano contratado. Fica guardada dentro do membro. */
public class Assinatura implements Cobravel {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String membroId;
    private TipoPlano tipo;
    private LocalDate inicio;
    private LocalDate fim;
    private BigDecimal valorPago;
    private LocalDateTime contratadaEm;

    protected Assinatura() {
    }

    /** Cria a assinatura começando em "inicio" e durando o número de dias do plano. */
    public Assinatura(String membroId, TipoPlano tipo, LocalDate inicio, LocalDateTime contratadaEm) {
        this.membroId = membroId;
        this.tipo = tipo;
        this.inicio = inicio;
        this.fim = inicio.plusDays(tipo.getDuracaoDias() - 1L);
        this.valorPago = tipo.getPreco();
        this.contratadaEm = contratadaEm;
    }

    public boolean estaAtivaEm(LocalDate data) {
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    public boolean isFutura(LocalDate hoje) {
        return inicio.isAfter(hoje);
    }

    @Override
    public TipoCobranca getTipoCobranca() {
        return TipoCobranca.PLANO;
    }

    @Override
    public String getDescricaoCobranca() {
        return "Plano " + tipo.getDescricao() + " (" + inicio.format(DATA) + " a " + fim.format(DATA) + ")";
    }

    @Override
    public BigDecimal getValorCobrado() {
        return valorPago;
    }

    @Override
    public String getReferenciaCobranca() {
        return membroId;
    }

    public TipoPlano getTipo() {
        return tipo;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public LocalDateTime getContratadaEm() {
        return contratadaEm;
    }
}
