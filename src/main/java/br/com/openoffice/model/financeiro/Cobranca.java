package br.com.openoffice.model.financeiro;

import br.com.openoffice.enums.StatusCobranca;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.interfaces.Cobravel;
import br.com.openoffice.model.Entidade;
import br.com.openoffice.util.Dinheiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Valor devido por um membro (plano, reserva, evento ou multa).
 * Uma cobrança é quitada por um {@link Pagamento}.
 */
public class Cobranca extends Entidade {

    private String membroId;
    private TipoCobranca tipo;
    private String referenciaId;
    private String descricao;
    private BigDecimal valor;
    private StatusCobranca status;
    private LocalDateTime criadaEm;
    private LocalDateTime pagaEm;
    private String pagamentoId;
    private BigDecimal valorEstornado;
    private LocalDateTime estornadaEm;

    protected Cobranca() {
    }

    public Cobranca(String membroId, Cobravel item, LocalDateTime agora) {
        this(membroId, item.getTipoCobranca(), item.getReferenciaCobranca(), item.getDescricaoCobranca(),
                item.getValorCobrado(), agora);
    }

    public Cobranca(String membroId, TipoCobranca tipo, String referenciaId, String descricao,
                    BigDecimal valor, LocalDateTime agora) {
        this.membroId = membroId;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.descricao = descricao;
        this.valor = Dinheiro.arredondar(valor);
        this.status = StatusCobranca.PENDENTE;
        this.criadaEm = agora;
        this.valorEstornado = Dinheiro.ZERO;
    }

    public void marcarComoPaga(String pagamentoId, LocalDateTime agora) {
        if (status != StatusCobranca.PENDENTE) {
            throw new DadosInvalidosException("Esta cobrança não está pendente.");
        }
        this.status = StatusCobranca.PAGA;
        this.pagamentoId = pagamentoId;
        this.pagaEm = agora;
    }

    /** Devolve todo o valor ao membro (cancelamento). */
    public void estornar(LocalDateTime agora) {
        if (status != StatusCobranca.PAGA) {
            throw new DadosInvalidosException("Só é possível estornar cobranças pagas.");
        }
        this.status = StatusCobranca.ESTORNADA;
        this.valorEstornado = valor;
        this.estornadaEm = agora;
    }

    public void cancelar() {
        if (status == StatusCobranca.PENDENTE) {
            this.status = StatusCobranca.CANCELADA;
        }
    }

    /** Valor que efetivamente ficou com o coworking (entra na receita). */
    public BigDecimal getValorLiquido() {
        return switch (status) {
            case PAGA -> valor;
            case ESTORNADA -> valor.subtract(valorEstornado);
            default -> Dinheiro.ZERO;
        };
    }

    public boolean isPendente() {
        return status == StatusCobranca.PENDENTE;
    }

    /** Data usada nos relatórios: quando o dinheiro entrou (ou quando foi criada, se pendente). */
    public LocalDateTime getDataReferencia() {
        return pagaEm != null ? pagaEm : criadaEm;
    }

    public String getMembroId() {
        return membroId;
    }

    public TipoCobranca getTipo() {
        return tipo;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusCobranca getStatus() {
        return status;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getPagaEm() {
        return pagaEm;
    }

    public String getPagamentoId() {
        return pagamentoId;
    }

    public BigDecimal getValorEstornado() {
        return valorEstornado == null ? Dinheiro.ZERO : valorEstornado;
    }

    public LocalDateTime getEstornadaEm() {
        return estornadaEm;
    }
}
