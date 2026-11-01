package br.com.openoffice.service;

import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.AntecedenciaInvalidaException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.HorarioIndisponivelException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.DadosPagamento;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.regra.PoliticaCancelamento;
import br.com.openoffice.regra.RegrasCoworking;
import br.com.openoffice.repository.ReservaRepository;
import br.com.openoffice.service.dto.AgendaDia;
import br.com.openoffice.service.dto.ResultadoCancelamento;
import br.com.openoffice.service.dto.SolicitacaoReserva;
import br.com.openoffice.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Regras de reserva:
 * - sem sobreposição de horários no mesmo espaço;
 * - dentro da capacidade do espaço;
 * - antecedência máxima de 7 dias (30 para VIP);
 * - sala privada só para VIP (regra da própria classe SalaPrivada);
 * - cancelamento com menos de 24h retém 50% do valor.
 */
@Service
public class ReservaService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaRepository reservas;
    private final EspacoService espacos;
    private final FinanceiroService financeiro;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public ReservaService(ReservaRepository reservas, EspacoService espacos, FinanceiroService financeiro,
                          NotificacaoService notificacoes, AuditoriaService auditoria, Clock relogio) {
        this.reservas = reservas;
        this.espacos = espacos;
        this.financeiro = financeiro;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    /** Valida o pedido e devolve o valor calculado, sem gravar nada (usado na tela de resumo). */
    public Orcamento simular(Membro membro, SolicitacaoReserva pedido) {
        Espaco espaco = espacos.buscarAtivo(pedido.espacoId());
        validar(membro, espaco, pedido);
        return espaco.calcularValor(java.time.Duration.between(pedido.inicio(), pedido.fim()),
                membro.isVip(hoje()));
    }

    public Reserva reservar(Membro membro, SolicitacaoReserva pedido, DadosPagamento pagamento) {
        Espaco espaco = espacos.buscarAtivo(pedido.espacoId());
        Orcamento orcamento = simular(membro, pedido);
        Reserva reserva = Reserva.deMembro(membro.getId(), espaco.getId(), espaco.getNome(),
                pedido.inicioCompleto(), pedido.fimCompleto(), pedido.participantes(), orcamento, agora());
        if (!orcamento.gratuito()) {
            // Valida o pagamento antes de gravar a reserva: cartão recusado = nada é salvo.
            financeiro.validarPagamento(pagamento);
        }
        reservas.salvar(reserva);
        Optional<Cobranca> cobranca = financeiro.cobrarEPagar(membro, reserva, pagamento);
        cobranca.ifPresent(c -> {
            reserva.vincularCobranca(c.getId());
            reservas.salvar(reserva);
        });
        auditoria.registrar(membro.getEmail(), "Reserva " + reserva.getId() + " criada: " + reserva.getDescricaoCobranca());
        return reserva;
    }

    private void validar(Membro membro, Espaco espaco, SolicitacaoReserva pedido) {
        if (pedido.data() == null || pedido.inicio() == null || pedido.fim() == null) {
            throw new DadosInvalidosException("Informe a data, o horário de início e o horário de fim.");
        }
        LocalDate hoje = hoje();
        validarHorario(pedido.inicio(), pedido.fim());
        if (!pedido.inicioCompleto().isAfter(agora())) {
            throw new DadosInvalidosException("Não é possível reservar um horário que já passou.");
        }

        espaco.validarAcesso(membro, hoje);

        int dias = membro.getDiasAntecedenciaMaxima(hoje);
        LocalDate limite = hoje.plusDays(dias);
        if (pedido.data().isAfter(limite)) {
            String plano = membro.getPlanoAtual(hoje).map(p -> p.getDescricao()).orElse("");
            throw new AntecedenciaInvalidaException("Data fora do prazo: no plano " + plano
                    + " é possível reservar com até " + dias + " dias de antecedência (até " + limite.format(DATA) + ")."
                    + (membro.isVip(hoje) ? "" : " Membros VIP podem reservar com até 30 dias."));
        }
        LocalDate fimCobertura = membro.getFimDaCobertura().orElse(hoje);
        if (pedido.data().isAfter(fimCobertura)) {
            throw new PlanoInativoException("Seu plano termina em " + fimCobertura.format(DATA)
                    + ". Renove o plano para reservar depois dessa data.");
        }

        espaco.validarCapacidade(pedido.participantes());

        List<Reserva> conflitos = reservas.conflitos(espaco.getId(), pedido.inicioCompleto(), pedido.fimCompleto());
        if (!conflitos.isEmpty()) {
            String ocupados = conflitos.stream()
                    .sorted(Comparator.comparing(Reserva::getInicio))
                    .map(r -> "das " + r.getInicio().format(HORA) + " às " + r.getFim().format(HORA))
                    .collect(Collectors.joining(", "));
            String sugestao = sugerirHorario(espaco, pedido)
                    .map(s -> " Horário livre mais próximo: " + s + ".")
                    .orElse(" Não há outro horário livre com essa duração neste dia.");
            throw new HorarioIndisponivelException("Horário indisponível: " + espaco.getNome()
                    + " já está reservado(a) " + ocupados + " neste dia." + sugestao);
        }
    }

    private void validarHorario(LocalTime inicio, LocalTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new DadosInvalidosException("O horário de fim precisa ser depois do início (" + inicio.format(HORA) + ").");
        }
        if (inicio.isBefore(RegrasCoworking.ABERTURA) || fim.isAfter(RegrasCoworking.FECHAMENTO)) {
            throw new DadosInvalidosException("As reservas devem ficar entre " + RegrasCoworking.ABERTURA.format(HORA)
                    + " e " + RegrasCoworking.FECHAMENTO.format(HORA) + ".");
        }
        if (inicio.getMinute() % RegrasCoworking.INTERVALO_MINUTOS != 0
                || fim.getMinute() % RegrasCoworking.INTERVALO_MINUTOS != 0) {
            throw new DadosInvalidosException("Use horários em intervalos de 30 minutos (ex.: 14:00, 14:30).");
        }
    }

    /** Procura o primeiro horário livre, a partir do pedido, com a mesma duração e no mesmo dia. */
    private Optional<String> sugerirHorario(Espaco espaco, SolicitacaoReserva pedido) {
        long minutos = java.time.Duration.between(pedido.inicio(), pedido.fim()).toMinutes();
        LocalTime candidato = pedido.inicio();
        while (!candidato.plusMinutes(minutos).isAfter(RegrasCoworking.FECHAMENTO)
                && candidato.plusMinutes(minutos).isAfter(candidato)) {
            LocalDateTime ini = pedido.data().atTime(candidato);
            LocalDateTime fim = ini.plusMinutes(minutos);
            if (ini.isAfter(agora()) && reservas.conflitos(espaco.getId(), ini, fim).isEmpty()) {
                return Optional.of(candidato.format(HORA) + "–" + fim.toLocalTime().format(HORA));
            }
            candidato = candidato.plusMinutes(RegrasCoworking.INTERVALO_MINUTOS);
        }
        return Optional.empty();
    }

    /** Mostra os valores do cancelamento antes de o membro confirmar. */
    public ResultadoCancelamento previaCancelamento(Membro membro, String reservaId) {
        Reserva reserva = buscarDoMembro(membro, reservaId);
        return calcularCancelamento(reserva);
    }

    public ResultadoCancelamento cancelar(Membro membro, String reservaId) {
        Reserva reserva = buscarDoMembro(membro, reservaId);
        ResultadoCancelamento resultado = calcularCancelamento(reserva);
        reserva.cancelar(agora(), resultado.multa());
        reservas.salvar(reserva);
        financeiro.estornarComMulta(reserva.getCobrancaId(), resultado.multa(),
                "Multa de 50% (cancelamento com menos de 24h): " + reserva.getEspacoNome() + " em "
                        + reserva.getInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        auditoria.registrar(membro.getEmail(), "Reserva " + reservaId + " cancelada. Multa: R$ " + resultado.multa());
        return resultado;
    }

    /** O administrador cancela sem multa (ex.: manutenção na sala) e o membro é avisado. */
    public void cancelarPeloAdministrador(String reservaId, String motivo) {
        Reserva reserva = buscar(reservaId);
        if (reserva.isDeEvento()) {
            throw new DadosInvalidosException("Esta reserva pertence a um evento e não pode ser cancelada separadamente.");
        }
        reserva.cancelar(agora(), Dinheiro.ZERO);
        reservas.salvar(reserva);
        financeiro.estornarComMulta(reserva.getCobrancaId(), Dinheiro.ZERO, "");
        String texto = "Sua reserva de " + reserva.getEspacoNome() + " em "
                + reserva.getInicio().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"))
                + " foi cancelada pela administração, com reembolso integral."
                + (motivo == null || motivo.isBlank() ? "" : " Motivo: " + motivo.trim());
        notificacoes.notificar(reserva.getMembroId(), texto);
        auditoria.registrar("admin", "Reserva " + reservaId + " cancelada pela administração");
    }

    private ResultadoCancelamento calcularCancelamento(Reserva reserva) {
        if (!reserva.isAtiva()) {
            throw new DadosInvalidosException("Esta reserva já foi cancelada.");
        }
        LocalDateTime agora = agora();
        if (!agora.isBefore(reserva.getInicio())) {
            throw new DadosInvalidosException("Não é possível cancelar uma reserva que já começou.");
        }
        BigDecimal pago = reserva.getValorTotal();
        BigDecimal multa = PoliticaCancelamento.calcularMulta(pago, reserva.getInicio(), agora);
        return new ResultadoCancelamento(reserva.getDescricaoCobranca(), pago, multa, pago.subtract(multa),
                PoliticaCancelamento.horasAteInicio(reserva.getInicio(), agora),
                PoliticaCancelamento.cobraMulta(reserva.getInicio(), agora));
    }

    public Reserva buscar(String id) {
        return reservas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva " + id + " não encontrada."));
    }

    private Reserva buscarDoMembro(Membro membro, String reservaId) {
        Reserva reserva = buscar(reservaId);
        if (!membro.getId().equals(reserva.getMembroId())) {
            throw new AcessoNegadoException("Esta reserva pertence a outro membro.");
        }
        return reserva;
    }

    public List<Reserva> doMembro(String membroId) {
        return reservas.doMembro(membroId).stream()
                .sorted(Comparator.comparing(Reserva::getInicio).reversed())
                .toList();
    }

    public List<Reserva> proximasDoMembro(String membroId, int quantidade) {
        LocalDateTime agora = agora();
        return reservas.doMembro(membroId).stream()
                .filter(r -> r.isAtiva() && r.getFim().isAfter(agora))
                .sorted(Comparator.comparing(Reserva::getInicio))
                .limit(quantidade)
                .toList();
    }

    public List<Reserva> todas() {
        return reservas.listarTodos().stream()
                .sorted(Comparator.comparing(Reserva::getInicio).reversed())
                .toList();
    }

    public List<Reserva> doDia(LocalDate dia) {
        return reservas.ativasNoPeriodo(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).stream()
                .sorted(Comparator.comparing(Reserva::getInicio))
                .toList();
    }

    /** Monta a grade de ocupação do dia. "membroId" destaca as reservas do próprio membro. */
    public AgendaDia agenda(LocalDate dia, String membroId) {
        int abertura = RegrasCoworking.ABERTURA.getHour();
        int fechamento = RegrasCoworking.FECHAMENTO.getHour();
        List<Integer> horas = new ArrayList<>();
        for (int h = abertura; h < fechamento; h++) {
            horas.add(h);
        }
        List<Reserva> doDia = doDia(dia);
        LocalDateTime agora = agora();
        List<AgendaDia.Linha> linhas = new ArrayList<>();
        for (Espaco espaco : espacos.listarAtivos()) {
            List<AgendaDia.Celula> celulas = new ArrayList<>();
            for (int h : horas) {
                LocalDateTime ini = dia.atTime(h, 0);
                LocalDateTime fim = ini.plusHours(1);
                Optional<Reserva> ocupante = doDia.stream()
                        .filter(r -> r.getEspacoId().equals(espaco.getId()) && r.sobrepoe(ini, fim))
                        .findFirst();
                celulas.add(ocupante.map(r -> celulaOcupada(r, membroId, h))
                        .orElseGet(() -> !fim.isAfter(agora)
                                ? new AgendaDia.Celula(h, "passado", "Horário encerrado")
                                : new AgendaDia.Celula(h, "livre", "Livre: clique para reservar")));
            }
            linhas.add(new AgendaDia.Linha(espaco, celulas));
        }
        return new AgendaDia(dia, horas, linhas);
    }

    private AgendaDia.Celula celulaOcupada(Reserva r, String membroId, int hora) {
        String periodo = r.getInicio().format(HORA) + "–" + r.getFim().format(HORA);
        if (r.isDeEvento()) {
            return new AgendaDia.Celula(hora, "evento", "Evento " + periodo);
        }
        if (membroId != null && membroId.equals(r.getMembroId())) {
            return new AgendaDia.Celula(hora, "minha", "Sua reserva " + periodo);
        }
        return new AgendaDia.Celula(hora, "ocupado", "Reservado " + periodo);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
