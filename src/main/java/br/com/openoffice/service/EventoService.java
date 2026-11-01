package br.com.openoffice.service;

import br.com.openoffice.enums.StatusInscricao;
import br.com.openoffice.exception.CapacidadeExcedidaException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.EventoLotadoException;
import br.com.openoffice.exception.HorarioIndisponivelException;
import br.com.openoffice.exception.InscricaoNaoPermitidaException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.model.evento.Inscricao;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.regra.PoliticaCancelamento;
import br.com.openoffice.repository.EventoRepository;
import br.com.openoffice.repository.ReservaRepository;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.service.dto.NovoEvento;
import br.com.openoffice.service.dto.ResultadoCancelamento;
import br.com.openoffice.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Eventos e workshops:
 * - o espaço do evento é reservado (sem sobreposição) e a capacidade não passa da do espaço;
 * - inscrições abrem primeiro para VIP e depois para todos;
 * - VIP tem 15% de desconto em eventos especiais;
 * - evento lotado tem lista de espera; quando alguém cancela, o primeiro da fila ganha a vaga.
 */
@Service
public class EventoService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final EventoRepository eventos;
    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final EspacoService espacos;
    private final FinanceiroService financeiro;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public EventoService(EventoRepository eventos, ReservaRepository reservas, UsuarioRepository usuarios,
                         EspacoService espacos, FinanceiroService financeiro, NotificacaoService notificacoes,
                         AuditoriaService auditoria, Clock relogio) {
        this.eventos = eventos;
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.espacos = espacos;
        this.financeiro = financeiro;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public Evento criar(NovoEvento dados) {
        if (dados.inicio() == null || dados.fim() == null) {
            throw new DadosInvalidosException("Informe o início e o fim do evento.");
        }
        LocalDateTime agora = agora();
        if (!dados.inicio().isAfter(agora)) {
            throw new DadosInvalidosException("O evento precisa começar no futuro.");
        }
        if (!dados.fim().isAfter(dados.inicio())) {
            throw new DadosInvalidosException("O fim do evento precisa ser depois do início.");
        }
        if (dados.diasPrioridadeVip() < 0 || dados.diasPrioridadeVip() > 30) {
            throw new DadosInvalidosException("A prioridade VIP deve ser de 0 a 30 dias.");
        }
        Espaco espaco = espacos.buscarAtivo(dados.espacoId());
        if (!espaco.aceitaEventos()) {
            throw new DadosInvalidosException("Eventos só podem ser feitos em salas de reunião ou no auditório.");
        }
        if (dados.capacidade() > espaco.getCapacidade()) {
            throw new CapacidadeExcedidaException("Capacidade excedida: " + espaco.getNome() + " comporta até "
                    + espaco.getCapacidade() + " pessoas.");
        }
        List<Reserva> conflitos = reservas.conflitos(espaco.getId(), dados.inicio(), dados.fim());
        if (!conflitos.isEmpty()) {
            throw new HorarioIndisponivelException(espaco.getNome() + " já tem " + conflitos.size()
                    + " reserva(s) nesse horário. Escolha outro horário ou espaço.");
        }
        LocalDateTime aberturaGeral = agora.plusDays(dados.diasPrioridadeVip());
        if (aberturaGeral.isAfter(dados.inicio())) {
            aberturaGeral = dados.inicio();
        }
        Evento evento = new Evento(dados.titulo(), dados.descricao(), espaco.getId(), espaco.getNome(),
                dados.inicio(), dados.fim(), dados.capacidade(),
                dados.preco() == null ? Dinheiro.ZERO : Dinheiro.arredondar(dados.preco()),
                dados.especial(), aberturaGeral, agora);
        eventos.salvar(evento);
        Reserva reserva = reservas.salvar(Reserva.deEvento(evento.getId(), espaco.getId(), espaco.getNome(),
                dados.inicio(), dados.fim(), dados.capacidade(), agora));
        evento.vincularReserva(reserva.getId());
        eventos.salvar(evento);
        auditoria.registrar("admin", "Evento " + evento.getId() + " criado: " + evento.getTitulo());
        return evento;
    }

    /** Valor que o membro pagaria (mostrado antes da inscrição). */
    public Orcamento simularInscricao(Membro membro, String eventoId) {
        Evento evento = buscar(eventoId);
        return evento.calcularValor(membro.isVip(hoje()));
    }

    public Inscricao inscrever(Membro membro, String eventoId, DadosPagamento pagamento) {
        Evento evento = buscar(eventoId);
        validarInscricao(membro, evento);
        if (evento.isLotado()) {
            throw new EventoLotadoException("Evento lotado. Você pode entrar na lista de espera.");
        }
        Orcamento orcamento = evento.calcularValor(membro.isVip(hoje()));
        if (!orcamento.gratuito()) {
            financeiro.validarPagamento(pagamento);
        }
        Inscricao inscricao = new Inscricao(membro.getId(), membro.getNome(), evento, orcamento,
                StatusInscricao.CONFIRMADA, agora());
        Optional<Cobranca> cobranca = financeiro.cobrarEPagar(membro, inscricao, pagamento);
        cobranca.ifPresent(c -> inscricao.vincularCobranca(c.getId()));
        evento.adicionarInscricao(inscricao);
        eventos.salvar(evento);
        auditoria.registrar(membro.getEmail(), "Inscrição no evento " + evento.getId());
        return inscricao;
    }

    public int entrarNaListaEspera(Membro membro, String eventoId) {
        Evento evento = buscar(eventoId);
        validarInscricao(membro, evento);
        if (!evento.isLotado()) {
            throw new InscricaoNaoPermitidaException("Ainda há vagas neste evento. Faça a inscrição normalmente.");
        }
        evento.entrarNaListaEspera(membro.getId());
        eventos.salvar(evento);
        auditoria.registrar(membro.getEmail(), "Entrou na lista de espera do evento " + evento.getId());
        return evento.posicaoNaListaEspera(membro.getId());
    }

    public void sairDaListaEspera(Membro membro, String eventoId) {
        Evento evento = buscar(eventoId);
        evento.sairDaListaEspera(membro.getId());
        eventos.salvar(evento);
    }

    private void validarInscricao(Membro membro, Evento evento) {
        LocalDateTime agora = agora();
        if (!membro.temPlanoAtivo(hoje())) {
            throw new PlanoInativoException("Você precisa de um plano ativo para se inscrever em eventos.");
        }
        if (evento.isInscricoesEncerradas(agora)) {
            throw new InscricaoNaoPermitidaException("As inscrições para este evento já foram encerradas.");
        }
        if (evento.inscricaoAtivaDe(membro.getId()).isPresent()) {
            throw new InscricaoNaoPermitidaException("Você já está inscrito neste evento.");
        }
        if (evento.estaNaListaEspera(membro.getId())) {
            throw new InscricaoNaoPermitidaException("Você já está na lista de espera (posição "
                    + evento.posicaoNaListaEspera(membro.getId()) + ").");
        }
        if (!membro.isVip(hoje()) && !evento.isAbertoParaTodos(agora)) {
            throw new InscricaoNaoPermitidaException("Inscrições abertas só para membros VIP até "
                    + evento.getAberturaGeral().format(DATA_HORA) + ". Depois disso, abrem para todos.");
        }
    }

    public ResultadoCancelamento previaCancelamento(Membro membro, String eventoId) {
        Evento evento = buscar(eventoId);
        Inscricao inscricao = inscricaoDo(membro, evento);
        return calcularCancelamento(evento, inscricao);
    }

    public ResultadoCancelamento cancelarInscricao(Membro membro, String eventoId) {
        Evento evento = buscar(eventoId);
        Inscricao inscricao = inscricaoDo(membro, evento);
        ResultadoCancelamento resultado = calcularCancelamento(evento, inscricao);
        inscricao.cancelar(resultado.multa());
        financeiro.estornarComMulta(inscricao.getCobrancaId(), resultado.multa(),
                "Multa de 50% (cancelamento com menos de 24h): evento " + evento.getTitulo());
        promoverListaEspera(evento);
        eventos.salvar(evento);
        auditoria.registrar(membro.getEmail(), "Cancelou a inscrição no evento " + eventoId);
        return resultado;
    }

    private ResultadoCancelamento calcularCancelamento(Evento evento, Inscricao inscricao) {
        LocalDateTime agora = agora();
        if (evento.isInscricoesEncerradas(agora)) {
            throw new InscricaoNaoPermitidaException("O evento já começou; não é possível cancelar a inscrição.");
        }
        boolean pagou = inscricao.getStatus() == StatusInscricao.CONFIRMADA;
        BigDecimal pago = pagou ? inscricao.getValor() : Dinheiro.ZERO;
        BigDecimal multa = PoliticaCancelamento.calcularMulta(pago, evento.getInicio(), agora);
        return new ResultadoCancelamento(inscricao.getDescricaoCobranca(), pago, multa, pago.subtract(multa),
                PoliticaCancelamento.horasAteInicio(evento.getInicio(), agora),
                PoliticaCancelamento.cobraMulta(evento.getInicio(), agora));
    }

    /** Chama os próximos da fila enquanto houver vaga. A vaga fica reservada até o pagamento. */
    private void promoverListaEspera(Evento evento) {
        while (!evento.isLotado()) {
            Optional<String> proximo = evento.chamarProximoDaEspera();
            if (proximo.isEmpty()) {
                return;
            }
            Optional<Membro> membro = usuarios.buscarMembro(proximo.get());
            if (membro.isEmpty() || !membro.get().temPlanoAtivo(hoje())) {
                continue;
            }
            Membro m = membro.get();
            Orcamento orcamento = evento.calcularValor(m.isVip(hoje()));
            boolean gratuito = orcamento.gratuito();
            Inscricao inscricao = new Inscricao(m.getId(), m.getNome(), evento, orcamento,
                    gratuito ? StatusInscricao.CONFIRMADA : StatusInscricao.AGUARDANDO_PAGAMENTO, agora());
            if (!gratuito) {
                Cobranca cobranca = financeiro.gerarCobrancaPendente(m, inscricao);
                inscricao.vincularCobranca(cobranca.getId());
            }
            evento.adicionarInscricao(inscricao);
            notificacoes.notificar(m.getId(), "Abriu uma vaga no evento \"" + evento.getTitulo() + "\" e ela é sua! "
                    + (gratuito ? "Sua inscrição está confirmada."
                    : "Pague a inscrição na página Pagamentos para confirmar."));
        }
    }

    /** Chamado depois que o membro paga a cobrança pendente de uma vaga da lista de espera. */
    public void confirmarPagamentoInscricao(Cobranca cobranca) {
        eventos.buscarPorId(cobranca.getReferenciaId()).ifPresent(evento ->
                evento.getInscricoesAtivas().stream()
                        .filter(i -> i.getMembroId().equals(cobranca.getMembroId())
                                && i.getStatus() == StatusInscricao.AGUARDANDO_PAGAMENTO)
                        .findFirst()
                        .ifPresent(i -> {
                            i.confirmar();
                            eventos.salvar(evento);
                        }));
    }

    private Inscricao inscricaoDo(Membro membro, Evento evento) {
        return evento.inscricaoAtivaDe(membro.getId())
                .orElseThrow(() -> new InscricaoNaoPermitidaException("Você não está inscrito neste evento."));
    }

    public Evento buscar(String id) {
        return eventos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento " + id + " não encontrado."));
    }

    /** Eventos que ainda não terminaram, do mais próximo para o mais distante. */
    public List<Evento> proximos() {
        LocalDateTime agora = agora();
        return eventos.filtrar(e -> e.getFim().isAfter(agora)).stream()
                .sorted(Comparator.comparing(Evento::getInicio))
                .toList();
    }

    public List<Evento> todos() {
        return eventos.listarTodos().stream()
                .sorted(Comparator.comparing(Evento::getInicio).reversed())
                .toList();
    }

    public List<Evento> doMembro(String membroId) {
        return eventos.filtrar(e -> e.inscricaoAtivaDe(membroId).isPresent() || e.estaNaListaEspera(membroId))
                .stream().sorted(Comparator.comparing(Evento::getInicio)).toList();
    }

    /** Eventos futuros em que o membro está inscrito ou na lista de espera. */
    public List<Evento> proximosDoMembro(String membroId) {
        LocalDateTime agora = agora();
        return doMembro(membroId).stream().filter(e -> e.getFim().isAfter(agora)).toList();
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
