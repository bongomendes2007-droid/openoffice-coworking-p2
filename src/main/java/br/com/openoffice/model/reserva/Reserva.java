package br.com.openoffice.model.reserva;

import br.com.openoffice.enums.StatusReserva;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.interfaces.Cobravel;
import br.com.openoffice.model.Entidade;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uso de um espaço num intervalo de tempo. Pode pertencer a um membro ou a um evento
 * (quando o administrador agenda um workshop, o espaço fica reservado para ele).
 */
public class Reserva extends Entidade implements Cobravel {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String espacoId;
    private String espacoNome;
    private String membroId;
    private String eventoId;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private int participantes;
    private BigDecimal valorBase;
    private int percentualDesconto;
    private BigDecimal desconto;
    private BigDecimal valorTotal;
    private StatusReserva status;
    private LocalDateTime criadaEm;
    private String cobrancaId;
    private LocalDateTime canceladaEm;
    private BigDecimal multa;
    private BigDecimal reembolso;

    protected Reserva() {
    }

    private Reserva(String espacoId, String espacoNome, LocalDateTime inicio, LocalDateTime fim,
                    int participantes, Orcamento orcamento, LocalDateTime criadaEm) {
        if (!fim.isAfter(inicio)) {
            throw new DadosInvalidosException("O horário de fim precisa ser depois do início.");
        }
        this.espacoId = espacoId;
        this.espacoNome = espacoNome;
        this.inicio = inicio;
        this.fim = fim;
        this.participantes = participantes;
        this.valorBase = orcamento.valorBase();
        this.percentualDesconto = orcamento.percentualDesconto();
        this.desconto = orcamento.desconto();
        this.valorTotal = orcamento.total();
        this.status = StatusReserva.CONFIRMADA;
        this.criadaEm = criadaEm;
    }

    /** Reserva feita por um membro. */
    public static Reserva deMembro(String membroId, String espacoId, String espacoNome, LocalDateTime inicio,
                                   LocalDateTime fim, int participantes, Orcamento orcamento, LocalDateTime agora) {
        Reserva r = new Reserva(espacoId, espacoNome, inicio, fim, participantes, orcamento, agora);
        r.membroId = membroId;
        return r;
    }

    /** Reserva que bloqueia o espaço para um evento (sem custo para membros). */
    public static Reserva deEvento(String eventoId, String espacoId, String espacoNome, LocalDateTime inicio,
                                   LocalDateTime fim, int participantes, LocalDateTime agora) {
        Reserva r = new Reserva(espacoId, espacoNome, inicio, fim, participantes,
                Orcamento.de(BigDecimal.ZERO, 0), agora);
        r.eventoId = eventoId;
        return r;
    }

    /** Verifica se esta reserva ocupa o espaço em algum momento do intervalo [inicio, fim). */
    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return isAtiva() && inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
    }

    public void cancelar(LocalDateTime agora, BigDecimal multa) {
        if (!isAtiva()) {
            throw new DadosInvalidosException("Esta reserva já foi cancelada.");
        }
        if (!agora.isBefore(inicio)) {
            throw new DadosInvalidosException("Não é possível cancelar uma reserva que já começou.");
        }
        this.status = StatusReserva.CANCELADA;
        this.canceladaEm = agora;
        this.multa = multa;
        this.reembolso = valorTotal.subtract(multa);
    }

    public boolean isAtiva() {
        return status == StatusReserva.CONFIRMADA;
    }

    public boolean isDeEvento() {
        return eventoId != null;
    }

    public boolean isConcluida(LocalDateTime agora) {
        return isAtiva() && !fim.isAfter(agora);
    }

    /** Situação para exibição: "Confirmada", "Concluída", "Em andamento" ou "Cancelada". */
    public String getSituacao(LocalDateTime agora) {
        if (!isAtiva()) {
            return status.getDescricao();
        }
        if (!fim.isAfter(agora)) {
            return "Concluída";
        }
        if (!inicio.isAfter(agora)) {
            return "Em andamento";
        }
        return status.getDescricao();
    }

    public Duration getDuracao() {
        return Duration.between(inicio, fim);
    }

    public void vincularCobranca(String cobrancaId) {
        this.cobrancaId = cobrancaId;
    }

    @Override
    public TipoCobranca getTipoCobranca() {
        return TipoCobranca.RESERVA;
    }

    @Override
    public String getDescricaoCobranca() {
        return "Reserva " + espacoNome + " em " + inicio.format(FORMATO) + " (" + participantes + " pessoa"
                + (participantes > 1 ? "s" : "") + ")";
    }

    @Override
    public BigDecimal getValorCobrado() {
        return valorTotal;
    }

    @Override
    public String getReferenciaCobranca() {
        return getId();
    }

    public String getEspacoId() {
        return espacoId;
    }

    public String getEspacoNome() {
        return espacoNome;
    }

    public String getMembroId() {
        return membroId;
    }

    public String getEventoId() {
        return eventoId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public int getParticipantes() {
        return participantes;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public int getPercentualDesconto() {
        return percentualDesconto;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public String getCobrancaId() {
        return cobrancaId;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public BigDecimal getMulta() {
        return multa == null ? Dinheiro.ZERO : multa;
    }

    public BigDecimal getReembolso() {
        return reembolso == null ? Dinheiro.ZERO : reembolso;
    }
}
