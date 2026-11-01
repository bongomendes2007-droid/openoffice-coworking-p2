package br.com.openoffice.service;

import br.com.openoffice.enums.StatusCobranca;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.interfaces.Cobravel;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.financeiro.Pagamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.repository.CobrancaRepository;
import br.com.openoffice.repository.PagamentoRepository;
import br.com.openoffice.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cobranças e pagamentos. Recebe qualquer {@link Cobravel} (plano, reserva ou inscrição),
 * então não precisa conhecer as regras de cada um.
 */
@Service
public class FinanceiroService {

    private final CobrancaRepository cobrancas;
    private final PagamentoRepository pagamentos;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public FinanceiroService(CobrancaRepository cobrancas, PagamentoRepository pagamentos,
                             AuditoriaService auditoria, Clock relogio) {
        this.cobrancas = cobrancas;
        this.pagamentos = pagamentos;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    /**
     * Valida o pagamento antes de gravar qualquer coisa. Se os dados do cartão forem recusados,
     * a exceção sai daqui e nada é salvo (nem a cobrança, nem o item cobrado).
     */
    public Pagamento prepararPagamento(DadosPagamento dados) {
        if (dados == null || dados.getForma() == null) {
            throw new DadosInvalidosException("Escolha a forma de pagamento.");
        }
        return dados.getForma().criarPagamento(dados);
    }

    /** Confere os dados do pagamento sem cobrar nada. Lança PagamentoRecusadoException se inválidos. */
    public void validarPagamento(DadosPagamento dados) {
        prepararPagamento(dados).conferirDados(LocalDateTime.now(relogio));
    }

    /** Cria a cobrança e quita na hora. Itens gratuitos não geram cobrança (retorna vazio). */
    public Optional<Cobranca> cobrarEPagar(Membro membro, Cobravel item, DadosPagamento dados) {
        if (!Dinheiro.positivo(item.getValorCobrado())) {
            return Optional.empty();
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        Pagamento pagamento = prepararPagamento(dados);
        Cobranca cobranca = new Cobranca(membro.getId(), item, agora);
        pagamento.processar(membro.getId(), cobranca.getValor(), agora);
        cobrancas.salvar(cobranca);
        registrarPagamento(cobranca, pagamento, agora);
        return Optional.of(cobranca);
    }

    /** Cria uma cobrança para ser paga depois (ex.: vaga liberada na lista de espera). */
    public Cobranca gerarCobrancaPendente(Membro membro, Cobravel item) {
        return cobrancas.salvar(new Cobranca(membro.getId(), item, LocalDateTime.now(relogio)));
    }

    /** Paga uma cobrança pendente do próprio membro. */
    public Cobranca pagar(Membro membro, String cobrancaId, DadosPagamento dados) {
        Cobranca cobranca = buscar(cobrancaId);
        if (!cobranca.getMembroId().equals(membro.getId())) {
            throw new AcessoNegadoException("Esta cobrança pertence a outro membro.");
        }
        if (!cobranca.isPendente()) {
            throw new DadosInvalidosException("Esta cobrança já está " + cobranca.getStatus().getDescricao()
                    .toLowerCase() + ".");
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        Pagamento pagamento = prepararPagamento(dados);
        pagamento.processar(membro.getId(), cobranca.getValor(), agora);
        registrarPagamento(cobranca, pagamento, agora);
        return cobranca;
    }

    private void registrarPagamento(Cobranca cobranca, Pagamento pagamento, LocalDateTime agora) {
        pagamento.vincularCobranca(cobranca.getId());
        Pagamento salvo = pagamentos.salvar(pagamento);
        cobranca.marcarComoPaga(salvo.getId(), agora);
        cobrancas.salvar(cobranca);
        auditoria.registrar(cobranca.getMembroId(), "Pagamento " + salvo.getId() + " de R$ " + cobranca.getValor()
                + " via " + salvo.getForma().getDescricao() + " (" + cobranca.getDescricao() + ")");
    }

    /**
     * Cancelamento: devolve o valor pago e, se houver multa, registra a multa como receita
     * (o coworking retém essa parte do valor que já tinha recebido).
     */
    public void estornarComMulta(String cobrancaId, BigDecimal multa, String descricaoMulta) {
        if (cobrancaId == null) {
            return;
        }
        Cobranca original = buscar(cobrancaId);
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (original.isPendente()) {
            original.cancelar();
            cobrancas.salvar(original);
            return;
        }
        if (original.getStatus() != StatusCobranca.PAGA) {
            return;
        }
        original.estornar(agora);
        cobrancas.salvar(original);
        if (Dinheiro.positivo(multa)) {
            Cobranca cobrancaMulta = new Cobranca(original.getMembroId(), TipoCobranca.MULTA,
                    original.getReferenciaId(), descricaoMulta, multa, agora);
            cobrancaMulta.marcarComoPaga(null, agora);
            cobrancas.salvar(cobrancaMulta);
        }
    }

    public Cobranca buscar(String id) {
        return cobrancas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobrança " + id + " não encontrada."));
    }

    public Optional<Pagamento> buscarPagamento(String id) {
        return pagamentos.buscarPorId(id);
    }

    public List<Cobranca> historico(String membroId) {
        return cobrancas.doMembro(membroId).stream()
                .sorted(Comparator.comparing(Cobranca::getCriadaEm).reversed())
                .toList();
    }

    public List<Cobranca> pendentes(String membroId) {
        return cobrancas.pendentesDoMembro(membroId);
    }

    public BigDecimal totalPago(String membroId) {
        return cobrancas.doMembro(membroId).stream()
                .map(Cobranca::getValorLiquido)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public List<Cobranca> todas() {
        return cobrancas.listarTodos();
    }
}
