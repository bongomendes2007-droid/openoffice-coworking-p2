package br.com.openoffice.model.evento;

import br.com.openoffice.enums.StatusInscricao;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.interfaces.Cobravel;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Participação de um membro em um evento. Fica guardada dentro do evento. */
public class Inscricao implements Cobravel {

    private String membroId;
    private String membroNome;
    private String eventoId;
    private String eventoTitulo;
    private BigDecimal valorBase;
    private int percentualDesconto;
    private BigDecimal valor;
    private StatusInscricao status;
    private LocalDateTime inscritoEm;
    private String cobrancaId;
    private BigDecimal multa;

    protected Inscricao() {
    }

    public Inscricao(String membroId, String membroNome, Evento evento, Orcamento orcamento,
                     StatusInscricao status, LocalDateTime agora) {
        this.membroId = membroId;
        this.membroNome = membroNome;
        this.eventoId = evento.getId();
        this.eventoTitulo = evento.getTitulo();
        this.valorBase = orcamento.valorBase();
        this.percentualDesconto = orcamento.percentualDesconto();
        this.valor = orcamento.total();
        this.status = status;
        this.inscritoEm = agora;
    }

    public boolean ocupaVaga() {
        return status.isOcupaVaga();
    }

    public void confirmar() {
        this.status = StatusInscricao.CONFIRMADA;
    }

    public void cancelar(BigDecimal multa) {
        this.status = StatusInscricao.CANCELADA;
        this.multa = multa;
    }

    public void vincularCobranca(String cobrancaId) {
        this.cobrancaId = cobrancaId;
    }

    @Override
    public TipoCobranca getTipoCobranca() {
        return TipoCobranca.EVENTO;
    }

    @Override
    public String getDescricaoCobranca() {
        return "Inscrição no evento " + eventoTitulo;
    }

    @Override
    public BigDecimal getValorCobrado() {
        return valor;
    }

    @Override
    public String getReferenciaCobranca() {
        return eventoId;
    }

    public String getMembroId() {
        return membroId;
    }

    public String getMembroNome() {
        return membroNome;
    }

    public String getEventoId() {
        return eventoId;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public int getPercentualDesconto() {
        return percentualDesconto;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusInscricao getStatus() {
        return status;
    }

    public LocalDateTime getInscritoEm() {
        return inscritoEm;
    }

    public String getCobrancaId() {
        return cobrancaId;
    }

    public BigDecimal getMulta() {
        return multa == null ? Dinheiro.ZERO : multa;
    }
}
