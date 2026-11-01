package br.com.openoffice.service;

import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.model.visita.Visita;
import br.com.openoffice.regra.RegrasCoworking;
import br.com.openoffice.report.GraficoDados;
import br.com.openoffice.report.RelatorioTabela;
import br.com.openoffice.report.TipoRelatorio;
import br.com.openoffice.repository.CobrancaRepository;
import br.com.openoffice.repository.EspacoRepository;
import br.com.openoffice.repository.EventoRepository;
import br.com.openoffice.repository.ReservaRepository;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.repository.VisitaRepository;
import br.com.openoffice.util.Dinheiro;
import br.com.openoffice.util.Formatador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Gera os relatórios de ocupação, financeiro, visitas e eventos a partir dos dados gravados. */
@Service
public class RelatorioService {

    public static final int MAX_DIAS_PERIODO = 366;

    private final ReservaRepository reservas;
    private final EspacoRepository espacos;
    private final CobrancaRepository cobrancas;
    private final VisitaRepository visitas;
    private final EventoRepository eventos;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public RelatorioService(ReservaRepository reservas, EspacoRepository espacos, CobrancaRepository cobrancas,
                            VisitaRepository visitas, EventoRepository eventos, UsuarioRepository usuarios,
                            Clock relogio) {
        this.reservas = reservas;
        this.espacos = espacos;
        this.cobrancas = cobrancas;
        this.visitas = visitas;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    /** Polimorfismo pelo enum: cada tipo de relatório chama o seu método. */
    public RelatorioTabela gerar(TipoRelatorio tipo, LocalDate de, LocalDate ate) {
        validarPeriodo(de, ate);
        return switch (tipo) {
            case OCUPACAO_SALA -> ocupacaoPorSala(de, ate);
            case OCUPACAO_HORARIO -> ocupacaoPorHorario(de, ate);
            case FINANCEIRO -> financeiro(de, ate);
            case VISITAS -> visitas(de, ate);
            case EVENTOS -> eventos(de, ate);
        };
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (de == null || ate == null) {
            throw new DadosInvalidosException("Informe as datas inicial e final do período.");
        }
        if (de.isAfter(ate)) {
            throw new DadosInvalidosException("A data inicial precisa ser anterior ou igual à data final.");
        }
        if (ChronoUnit.DAYS.between(de, ate) >= MAX_DIAS_PERIODO) {
            throw new DadosInvalidosException("Escolha um período de no máximo " + MAX_DIAS_PERIODO + " dias.");
        }
    }

    public RelatorioTabela ocupacaoPorSala(LocalDate de, LocalDate ate) {
        long dias = ChronoUnit.DAYS.between(de, ate) + 1;
        double horasDisponiveis = dias * RegrasCoworking.horasPorDia();
        List<Reserva> doPeriodo = reservas.ativasNoPeriodo(de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        RelatorioTabela rel = new RelatorioTabela("Ocupação por sala", periodo(de, ate) + " · base de "
                + Formatador.horas(horasDisponiveis) + " por espaço (" + dias + " dias, "
                + RegrasCoworking.ABERTURA.getHour() + "h às " + RegrasCoworking.FECHAMENTO.getHour() + "h)",
                List.of("Espaço", "Tipo", "Reservas", "Horas reservadas", "Ocupação"), agora());

        List<String> rotulos = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        double somaHoras = 0;
        String maisUsado = "-";
        double maiorTaxa = -1;
        for (Espaco espaco : espacosOrdenados()) {
            List<Reserva> doEspaco = doPeriodo.stream().filter(r -> r.getEspacoId().equals(espaco.getId())).toList();
            double horas = doEspaco.stream()
                    .mapToDouble(r -> minutosNoPeriodo(r, de, ate) / 60.0)
                    .sum();
            double taxa = horasDisponiveis == 0 ? 0 : horas * 100 / horasDisponiveis;
            somaHoras += horas;
            if (taxa > maiorTaxa) {
                maiorTaxa = taxa;
                maisUsado = espaco.getNome();
            }
            rel.adicionarLinha(espaco.getNome(), espaco.getTipo().getDescricao(), doEspaco.size(),
                    Formatador.horas(horas), Formatador.percentual(taxa));
            rotulos.add(espaco.getNome());
            valores.add(Math.round(taxa * 10) / 10.0);
        }
        int quantidade = rotulos.size();
        rel.adicionarResumo("Total de horas reservadas", Formatador.horas(somaHoras));
        rel.adicionarResumo("Ocupação média", Formatador.percentual(quantidade == 0 ? 0
                : somaHoras * 100 / (horasDisponiveis * quantidade)));
        rel.adicionarResumo("Espaço mais utilizado", maisUsado);
        return rel.comGrafico(new GraficoDados("Taxa de ocupação (%)", rotulos, valores, "%"));
    }

    public RelatorioTabela ocupacaoPorHorario(LocalDate de, LocalDate ate) {
        int abertura = RegrasCoworking.ABERTURA.getHour();
        int fechamento = RegrasCoworking.FECHAMENTO.getHour();
        List<String> colunas = new ArrayList<>();
        colunas.add("Espaço");
        for (int h = abertura; h < fechamento; h++) {
            colunas.add(String.format("%02dh", h));
        }
        colunas.add("Total");
        RelatorioTabela rel = new RelatorioTabela("Ocupação por horário e sala", periodo(de, ate)
                + " · cada célula mostra em quantos dias aquele horário esteve ocupado", colunas, agora());

        List<Reserva> doPeriodo = reservas.ativasNoPeriodo(de.atStartOfDay(), ate.plusDays(1).atStartOfDay());
        int[] totalPorHora = new int[fechamento - abertura];
        for (Espaco espaco : espacosOrdenados()) {
            List<Reserva> doEspaco = doPeriodo.stream().filter(r -> r.getEspacoId().equals(espaco.getId())).toList();
            List<Object> linha = new ArrayList<>();
            linha.add(espaco.getNome());
            int total = 0;
            for (int h = abertura; h < fechamento; h++) {
                int ocupados = 0;
                for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
                    LocalDateTime ini = dia.atTime(h, 0);
                    LocalDateTime fim = ini.plusHours(1);
                    if (doEspaco.stream().anyMatch(r -> r.sobrepoe(ini, fim))) {
                        ocupados++;
                    }
                }
                linha.add(ocupados);
                totalPorHora[h - abertura] += ocupados;
                total += ocupados;
            }
            linha.add(total);
            rel.adicionarLinha(linha.toArray());
        }

        List<String> rotulos = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        int pico = 0;
        int calmo = 0;
        for (int i = 0; i < totalPorHora.length; i++) {
            rotulos.add(String.format("%02dh", abertura + i));
            valores.add(totalPorHora[i]);
            if (totalPorHora[i] > totalPorHora[pico]) {
                pico = i;
            }
            if (totalPorHora[i] < totalPorHora[calmo]) {
                calmo = i;
            }
        }
        rel.adicionarResumo("Horário de pico", String.format("%02dh às %02dh", abertura + pico, abertura + pico + 1));
        rel.adicionarResumo("Horário mais tranquilo", String.format("%02dh às %02dh", abertura + calmo,
                abertura + calmo + 1));
        return rel.comoMapaDeCalor().comGrafico(new GraficoDados("Horas ocupadas por faixa de horário (todas as salas)",
                rotulos, valores, "h"));
    }

    public RelatorioTabela financeiro(LocalDate de, LocalDate ate) {
        LocalDateTime ini = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        List<Cobranca> doPeriodo = cobrancas.filtrar(c -> !c.getDataReferencia().isBefore(ini)
                        && c.getDataReferencia().isBefore(fim)).stream()
                .sorted(Comparator.comparing(Cobranca::getDataReferencia))
                .toList();
        RelatorioTabela rel = new RelatorioTabela("Relatório financeiro", periodo(de, ate),
                List.of("Data", "Membro", "Tipo", "Descrição", "Situação", "Valor", "Receita"), agora());

        Map<TipoCobranca, BigDecimal> porTipo = new EnumMap<>(TipoCobranca.class);
        for (TipoCobranca t : TipoCobranca.values()) {
            porTipo.put(t, Dinheiro.ZERO);
        }
        BigDecimal pendente = Dinheiro.ZERO;
        BigDecimal estornado = Dinheiro.ZERO;
        for (Cobranca c : doPeriodo) {
            rel.adicionarLinha(Formatador.data(c.getDataReferencia()), nomeDoMembro(c.getMembroId()),
                    c.getTipo().getDescricao(), c.getDescricao(), c.getStatus().getDescricao(),
                    Formatador.moeda(c.getValor()), Formatador.moeda(c.getValorLiquido()));
            porTipo.merge(c.getTipo(), c.getValorLiquido(), BigDecimal::add);
            if (c.isPendente()) {
                pendente = pendente.add(c.getValor());
            }
            estornado = estornado.add(c.getValorEstornado());
        }
        BigDecimal total = porTipo.values().stream().reduce(Dinheiro.ZERO, BigDecimal::add);
        porTipo.forEach((tipo, valor) -> rel.adicionarResumo("Receita com " + tipo.getDescricao().toLowerCase(),
                Formatador.moeda(valor)));
        rel.adicionarResumo("Reembolsos", Formatador.moeda(estornado));
        rel.adicionarResumo("A receber (pendente)", Formatador.moeda(pendente));
        rel.adicionarResumo("Receita total", Formatador.moeda(total));
        return rel.comGrafico(new GraficoDados("Receita por origem (R$)",
                porTipo.keySet().stream().map(TipoCobranca::getDescricao).toList(),
                porTipo.values().stream().map(v -> (Number) v.doubleValue()).toList(), "R$"));
    }

    public RelatorioTabela visitas(LocalDate de, LocalDate ate) {
        LocalDateTime ini = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        LocalDateTime agora = agora();
        List<Visita> doPeriodo = visitas.filtrar(v -> !v.getEntrada().isBefore(ini) && v.getEntrada().isBefore(fim))
                .stream().sorted(Comparator.comparing(Visita::getEntrada).reversed()).toList();
        RelatorioTabela rel = new RelatorioTabela("Histórico de visitas", periodo(de, ate),
                List.of("Visitante", "Documento", "Autorizado por", "Entrada", "Saída", "Permanência"), agora);
        for (Visita v : doPeriodo) {
            rel.adicionarLinha(v.getVisitanteNome(), v.getVisitanteDocumento(), v.getMembroNome(),
                    Formatador.dataHora(v.getEntrada()),
                    v.getSaida() == null ? "No espaço agora" : Formatador.dataHora(v.getSaida()),
                    Formatador.duracao(v.getPermanencia(agora)));
        }
        Map<String, Long> porMembro = doPeriodo.stream()
                .collect(Collectors.groupingBy(Visita::getMembroNome, Collectors.counting()));
        List<Map.Entry<String, Long>> ranking = porMembro.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .toList();
        long media = doPeriodo.isEmpty() ? 0 : (long) doPeriodo.stream()
                .mapToLong(v -> v.getPermanencia(agora).toMinutes()).average().orElse(0);
        rel.adicionarResumo("Total de visitas", String.valueOf(doPeriodo.size()));
        rel.adicionarResumo("Visitantes diferentes", String.valueOf(doPeriodo.stream()
                .map(Visita::getVisitanteId).distinct().count()));
        rel.adicionarResumo("Permanência média", Formatador.duracao(Duration.ofMinutes(media)));
        rel.adicionarResumo("Membro que mais recebeu visitas", ranking.isEmpty() ? "-" : ranking.get(0).getKey());
        return rel.comGrafico(new GraficoDados("Visitas por membro", ranking.stream().map(Map.Entry::getKey).toList(),
                ranking.stream().map(e -> (Number) e.getValue()).toList(), ""));
    }

    public RelatorioTabela eventos(LocalDate de, LocalDate ate) {
        LocalDateTime ini = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        List<Evento> doPeriodo = eventos.filtrar(e -> !e.getInicio().isBefore(ini) && e.getInicio().isBefore(fim))
                .stream().sorted(Comparator.comparing(Evento::getInicio)).toList();
        RelatorioTabela rel = new RelatorioTabela("Eventos e workshops", periodo(de, ate),
                List.of("Evento", "Data", "Espaço", "Inscritos", "Ocupação", "Lista de espera", "Receita"), agora());
        BigDecimal receitaTotal = Dinheiro.ZERO;
        int inscritosTotal = 0;
        List<String> rotulos = new ArrayList<>();
        List<Number> valores = new ArrayList<>();
        for (Evento e : doPeriodo) {
            BigDecimal receita = cobrancas.filtrar(c -> e.getId().equals(c.getReferenciaId())
                            && (c.getTipo() == TipoCobranca.EVENTO || c.getTipo() == TipoCobranca.MULTA))
                    .stream().map(Cobranca::getValorLiquido).reduce(Dinheiro.ZERO, BigDecimal::add);
            double taxa = e.getVagasOcupadas() * 100.0 / e.getCapacidade();
            receitaTotal = receitaTotal.add(receita);
            inscritosTotal += e.getVagasOcupadas();
            rel.adicionarLinha(e.getTitulo() + (e.isEspecial() ? " (especial)" : ""), Formatador.dataHora(e.getInicio()),
                    e.getEspacoNome(), e.getVagasOcupadas() + "/" + e.getCapacidade(), Formatador.percentual(taxa),
                    e.getListaEspera().size(), Formatador.moeda(receita));
            rotulos.add(e.getTitulo());
            valores.add(Math.round(taxa));
        }
        rel.adicionarResumo("Eventos no período", String.valueOf(doPeriodo.size()));
        rel.adicionarResumo("Total de inscritos", String.valueOf(inscritosTotal));
        rel.adicionarResumo("Receita com eventos", Formatador.moeda(receitaTotal));
        return rel.comGrafico(new GraficoDados("Ocupação das vagas (%)", rotulos, valores, "%"));
    }

    /** Minutos da reserva que caem dentro do período (uma reserva pode começar antes ou terminar depois). */
    private long minutosNoPeriodo(Reserva r, LocalDate de, LocalDate ate) {
        LocalDateTime ini = r.getInicio().isBefore(de.atStartOfDay()) ? de.atStartOfDay() : r.getInicio();
        LocalDateTime limite = ate.plusDays(1).atStartOfDay();
        LocalDateTime fim = r.getFim().isAfter(limite) ? limite : r.getFim();
        return Math.max(0, Duration.between(ini, fim).toMinutes());
    }

    private List<Espaco> espacosOrdenados() {
        return espacos.listarTodos().stream()
                .sorted(Comparator.comparing(Espaco::getTipo).thenComparing(Espaco::getNome))
                .toList();
    }

    private String nomeDoMembro(String id) {
        return usuarios.buscarPorId(id).map(Usuario::getNome).orElse("(removido)");
    }

    private String periodo(LocalDate de, LocalDate ate) {
        return "Período: " + Formatador.data(de) + " a " + Formatador.data(ate);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
