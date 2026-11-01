package br.com.openoffice.config;

import br.com.openoffice.enums.StatusInscricao;
import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.interfaces.Cobravel;
import br.com.openoffice.model.Notificacao;
import br.com.openoffice.model.espaco.Auditorio;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.espaco.EstacaoTrabalho;
import br.com.openoffice.model.espaco.SalaPrivada;
import br.com.openoffice.model.espaco.SalaReuniao;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.model.evento.Inscricao;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.model.financeiro.Pagamento;
import br.com.openoffice.model.financeiro.PagamentoBoleto;
import br.com.openoffice.model.financeiro.PagamentoPix;
import br.com.openoffice.model.pessoa.Administrador;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Visitante;
import br.com.openoffice.model.plano.Assinatura;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.model.visita.Visita;
import br.com.openoffice.regra.PoliticaCancelamento;
import br.com.openoffice.repository.ArmazenamentoJson;
import br.com.openoffice.repository.CobrancaRepository;
import br.com.openoffice.repository.EspacoRepository;
import br.com.openoffice.repository.EventoRepository;
import br.com.openoffice.repository.NotificacaoRepository;
import br.com.openoffice.repository.PagamentoRepository;
import br.com.openoffice.repository.ReservaRepository;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.repository.VisitaRepository;
import br.com.openoffice.repository.VisitanteRepository;
import br.com.openoffice.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ao iniciar: faz backup dos arquivos de dados e, se for a primeira execução (sem usuários),
 * cria dados de demonstração para a apresentação (membros, espaços, reservas, eventos e visitas).
 */
@Component
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);
    private static final String SENHA_PADRAO = "123456";

    private final ArmazenamentoJson armazenamento;
    private final UsuarioRepository usuarios;
    private final EspacoRepository espacos;
    private final ReservaRepository reservas;
    private final EventoRepository eventos;
    private final CobrancaRepository cobrancas;
    private final PagamentoRepository pagamentos;
    private final VisitanteRepository visitantes;
    private final VisitaRepository visitas;
    private final NotificacaoRepository notificacoes;
    private final AuditoriaService auditoria;
    private final PasswordEncoder codificador;
    private final Clock relogio;
    private final boolean criarDemonstracao;

    private final Random aleatorio = new Random(42);
    private final List<Reserva> todasReservas = new ArrayList<>();
    private LocalDateTime agora;
    private LocalDate hoje;

    public DadosIniciais(ArmazenamentoJson armazenamento, UsuarioRepository usuarios, EspacoRepository espacos,
                         ReservaRepository reservas, EventoRepository eventos, CobrancaRepository cobrancas,
                         PagamentoRepository pagamentos, VisitanteRepository visitantes, VisitaRepository visitas,
                         NotificacaoRepository notificacoes, AuditoriaService auditoria, PasswordEncoder codificador,
                         Clock relogio, @Value("${openoffice.dados-demonstracao:true}") boolean criarDemonstracao) {
        this.armazenamento = armazenamento;
        this.usuarios = usuarios;
        this.espacos = espacos;
        this.reservas = reservas;
        this.eventos = eventos;
        this.cobrancas = cobrancas;
        this.pagamentos = pagamentos;
        this.visitantes = visitantes;
        this.visitas = visitas;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.codificador = codificador;
        this.relogio = relogio;
        this.criarDemonstracao = criarDemonstracao;
    }

    @Override
    public void run(ApplicationArguments args) {
        agora = LocalDateTime.now(relogio).withSecond(0).withNano(0);
        hoje = agora.toLocalDate();
        Path backup = armazenamento.criarBackup(agora);
        if (backup != null) {
            log.info("Backup dos dados criado em {}", backup);
        }
        if (usuarios.contar() > 0) {
            return;
        }
        if (!criarDemonstracao) {
            usuarios.salvar(new Administrador("Recepção OpenOffice", "admin@openoffice.com",
                    codificador.encode("admin123"), agora));
            return;
        }
        log.info("Primeira execução: criando dados de demonstração...");
        popular();
        auditoria.registrar("sistema", "Dados de demonstração criados");
        log.info("Dados de demonstração criados. Login admin: admin@openoffice.com / admin123");
    }

    private void popular() {
        String hash = codificador.encode(SENHA_PADRAO);
        usuarios.salvar(new Administrador("Recepção OpenOffice", "admin@openoffice.com",
                codificador.encode("admin123"), agora.minusDays(60)));

        Membro ana = membro("Ana Souza", "526.018.159-06", "ana@email.com", "(85) 99876-1020", hash);
        Membro bruno = membro("Bruno Lima", "083.016.613-05", "bruno@email.com", "(85) 98765-4321", hash);
        Membro carla = membro("Carla Mendes", "186.091.390-34", "carla@email.com", "(85) 99123-4567", hash);
        Membro diego = membro("Diego Rocha", "996.030.824-30", "diego@email.com", "(85) 98111-2233", hash);
        Membro elisa = membro("Elisa Martins", "628.194.821-12", "elisa@email.com", "(85) 99654-7788", hash);
        Membro felipe = membro("Felipe Costa", "993.518.190-19", "felipe@email.com", "(85) 98222-3344", hash);

        assinar(ana, TipoPlano.VIP, hoje.minusDays(48));
        assinar(ana, TipoPlano.VIP, hoje.minusDays(18));
        assinar(bruno, TipoPlano.MENSAL, hoje.minusDays(40));
        assinar(bruno, TipoPlano.MENSAL, hoje.minusDays(10));
        assinar(carla, TipoPlano.SEMANAL, hoje.minusDays(2));
        assinar(diego, TipoPlano.DIARIO, hoje.minusDays(6));
        assinar(elisa, TipoPlano.VIP, hoje.minusDays(25));
        assinar(felipe, TipoPlano.MENSAL, hoje.minusDays(20));
        List<Membro> membros = List.of(ana, bruno, carla, diego, elisa, felipe);
        membros.forEach(usuarios::salvar);

        List<Espaco> estacoes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            estacoes.add(espacos.salvar(new EstacaoTrabalho(String.format("Estação %02d", i),
                    "Mesa individual com monitor, tomada e cadeira ergonômica.")));
        }
        Espaco reuniao1 = espacos.salvar(new SalaReuniao("Sala Reunião 1", 8,
                "Mesa para 8 pessoas, TV 55\" e quadro branco."));
        Espaco reuniao2 = espacos.salvar(new SalaReuniao("Sala Reunião 2", 10,
                "Mesa para 10 pessoas, projetor e sistema de videoconferência."));
        Espaco privadaA = espacos.salvar(new SalaPrivada("Sala Privada A", 4,
                "Sala fechada com isolamento acústico, ideal para equipes pequenas."));
        Espaco privadaB = espacos.salvar(new SalaPrivada("Sala Privada B", 2,
                "Cabine para chamadas e atendimentos individuais."));
        Espaco auditorio = espacos.salvar(new Auditorio("Auditório", 120,
                "Palco, telão, som e 120 lugares. Usado em eventos e workshops."));

        criarEventos(reuniao1, reuniao2, auditorio, ana, bruno, carla, elisa, felipe);
        criarReservasFixas(ana, bruno, carla, elisa, felipe, estacoes, reuniao1, reuniao2, privadaA, auditorio);
        criarHistoricoDeReservas(membros, estacoes, List.of(reuniao1, reuniao2, privadaA, privadaB, auditorio));
        criarVisitas(ana, bruno, carla, elisa, felipe);

        notificar(ana, "Seu plano VIP vence em 11 dias. Renove em \"Meu plano\" para não perder os benefícios.");
        notificar(diego, "Seu plano Diário venceu. Assine um plano para voltar a reservar espaços.");
    }

    private Membro membro(String nome, String cpf, String email, String telefone, String hash) {
        Membro m = usuarios.salvar(new Membro(nome, cpf, email, telefone, hash, agora.minusDays(60)));
        notificar(m, "Boas-vindas ao OpenOffice Coworking!");
        return m;
    }

    private void assinar(Membro m, TipoPlano tipo, LocalDate inicio) {
        Assinatura a = new Assinatura(m.getId(), tipo, inicio, inicio.atTime(9, 0));
        m.adicionarAssinatura(a);
        pagar(m.getId(), a, inicio.atTime(9, 0), false);
    }

    /** Registra uma cobrança já paga, como se o membro tivesse pago naquela data. */
    private Cobranca pagar(String membroId, Cobravel item, LocalDateTime quando, boolean boleto) {
        Cobranca c = cobrancas.salvar(new Cobranca(membroId, item, quando));
        Pagamento p = boleto ? new PagamentoBoleto() : new PagamentoPix();
        p.processar(membroId, c.getValor(), quando);
        p.vincularCobranca(c.getId());
        pagamentos.salvar(p);
        c.marcarComoPaga(p.getId(), quando);
        return cobrancas.salvar(c);
    }

    private void criarEventos(Espaco reuniao1, Espaco reuniao2, Espaco auditorio, Membro ana, Membro bruno,
                              Membro carla, Membro elisa, Membro felipe) {
        Evento java = evento("Workshop de Java na prática", "Do zero à primeira API com Spring Boot. Traga o notebook.",
                auditorio, hoje.plusDays(9).atTime(19, 0), 3, 40, "150.00", true, agora.plusDays(2));
        inscrever(java, elisa, true);

        Evento networking = evento("Networking de Startups", "Rodada de apresentações rápidas entre fundadores.",
                reuniao2, hoje.plusDays(14).atTime(18, 30), 2, 4, "30.00", false, agora.minusDays(8));
        inscrever(networking, bruno, false);
        inscrever(networking, carla, false);
        inscrever(networking, elisa, true);
        inscrever(networking, felipe, false);

        Evento carreira = evento("Palestra: Carreira em TI", "Como montar um portfólio e se preparar para entrevistas.",
                auditorio, hoje.plusDays(19).atTime(19, 0), 2, 80, "0.00", false, agora.minusDays(1));
        inscrever(carreira, bruno, false);
        inscrever(carreira, carla, false);

        Evento cafe = evento("Café com Investidores", "Conversa aberta com investidores-anjo da região.",
                reuniao1, hoje.minusDays(12).atTime(8, 0), 2, 8, "40.00", true, agora.minusDays(20));
        for (Membro m : List.of(ana, bruno, carla, elisa, felipe)) {
            inscrever(cafe, m, m == ana || m == elisa);
        }
    }

    private Evento evento(String titulo, String descricao, Espaco espaco, LocalDateTime inicio, int horas,
                          int capacidade, String preco, boolean especial, LocalDateTime aberturaGeral) {
        LocalDateTime criadoEm = inicio.isBefore(agora) ? inicio.minusDays(20) : agora.minusDays(10);
        Evento e = eventos.salvar(new Evento(titulo, descricao, espaco.getId(), espaco.getNome(), inicio,
                inicio.plusHours(horas), capacidade, new BigDecimal(preco), especial, aberturaGeral, criadoEm));
        Reserva r = reservas.salvar(Reserva.deEvento(e.getId(), espaco.getId(), espaco.getNome(), inicio,
                inicio.plusHours(horas), capacidade, criadoEm));
        todasReservas.add(r);
        e.vincularReserva(r.getId());
        return eventos.salvar(e);
    }

    private void inscrever(Evento e, Membro m, boolean vip) {
        Orcamento o = e.calcularValor(vip);
        LocalDateTime quando = e.getCriadoEm().plusDays(1 + aleatorio.nextInt(3));
        Inscricao i = new Inscricao(m.getId(), m.getNome(), e, o, StatusInscricao.CONFIRMADA, quando);
        if (!o.gratuito()) {
            i.vincularCobranca(pagar(m.getId(), i, quando, false).getId());
        }
        e.adicionarInscricao(i);
        eventos.salvar(e);
    }

    private void criarReservasFixas(Membro ana, Membro bruno, Membro carla, Membro elisa, Membro felipe,
                                    List<Espaco> estacoes, Espaco reuniao1, Espaco reuniao2, Espaco privadaA,
                                    Espaco auditorio) {
        reservar(bruno, reuniao1, hoje, 10, 2, 5, false);
        reservar(ana, privadaA, hoje, 14, 3, 3, true);
        reservar(carla, estacoes.get(1), hoje, 8, 10, 1, false);
        reservar(elisa, auditorio, hoje, 19, 2, 40, true);
        reservar(ana, auditorio, hoje.plusDays(1), 9, 2, 30, true);
        reservar(bruno, reuniao2, hoje.plusDays(1), 15, 1, 4, false);
        reservar(felipe, reuniao1, hoje.plusDays(3), 9, 3, 6, false);
        reservar(ana, reuniao2, hoje.plusDays(4), 14, 2, 6, true);
        reservar(elisa, privadaA, hoje.plusDays(2), 10, 2, 2, true);
    }

    /** Gera um mês de reservas passadas para que os relatórios tenham dados. */
    private void criarHistoricoDeReservas(List<Membro> membros, List<Espaco> estacoes, List<Espaco> salas) {
        for (LocalDate dia = hoje.minusDays(30); dia.isBefore(hoje); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            int quantidade = 6 + aleatorio.nextInt(6);
            for (int i = 0; i < quantidade; i++) {
                boolean estacao = aleatorio.nextInt(4) == 0;
                Espaco espaco = estacao ? estacoes.get(aleatorio.nextInt(estacoes.size()))
                        : salas.get(pesoSalas());
                LocalDate data = dia;
                List<Membro> aptos = membros.stream()
                        .filter(m -> m.temPlanoAtivo(data) && (!espaco.isExclusivoVip() || m.isVip(data)))
                        .toList();
                if (aptos.isEmpty()) {
                    continue;
                }
                Membro m = aptos.get(aleatorio.nextInt(aptos.size()));
                int hora = horaComPico();
                int duracao = estacao ? 4 + aleatorio.nextInt(5) : 1 + aleatorio.nextInt(3);
                if (hora + duracao > 22) {
                    duracao = 22 - hora;
                }
                int pessoas = Math.max(1, Math.min(espaco.getCapacidade(), 2 + aleatorio.nextInt(7)));
                if (espaco.getCapacidade() > 20) {
                    pessoas = 15 + aleatorio.nextInt(60);
                }
                Reserva r = reservar(m, espaco, dia, hora, duracao, pessoas, m.isVip(dia));
                if (r != null && aleatorio.nextInt(14) == 0) {
                    cancelarComMulta(r);
                }
            }
        }
    }

    /** Salas de reunião aparecem mais vezes que o auditório. */
    private int pesoSalas() {
        int[] pesos = {0, 0, 0, 1, 1, 1, 2, 2, 3, 4};
        return pesos[aleatorio.nextInt(pesos.length)];
    }

    /** Horários mais comuns no meio da manhã e no meio da tarde. */
    private int horaComPico() {
        int[] horas = {8, 9, 9, 10, 10, 10, 11, 13, 14, 14, 14, 15, 15, 16, 17, 18, 19};
        return horas[aleatorio.nextInt(horas.length)];
    }

    private Reserva reservar(Membro m, Espaco espaco, LocalDate dia, int hora, int duracao, int pessoas, boolean vip) {
        LocalDateTime inicio = dia.atTime(LocalTime.of(hora, 0));
        LocalDateTime fim = inicio.plusHours(duracao);
        boolean conflito = todasReservas.stream()
                .anyMatch(r -> r.getEspacoId().equals(espaco.getId()) && r.sobrepoe(inicio, fim));
        if (conflito) {
            return null;
        }
        Orcamento o = espaco.calcularValor(Duration.ofHours(duracao), vip);
        LocalDateTime criadaEm = inicio.minusDays(1 + aleatorio.nextInt(4));
        if (criadaEm.isAfter(agora)) {
            criadaEm = agora.minusHours(2);
        }
        Reserva r = reservas.salvar(Reserva.deMembro(m.getId(), espaco.getId(), espaco.getNome(), inicio, fim,
                pessoas, o, criadaEm));
        if (!o.gratuito()) {
            r.vincularCobranca(pagar(m.getId(), r, criadaEm, aleatorio.nextInt(5) == 0).getId());
            reservas.salvar(r);
        }
        todasReservas.add(r);
        return r;
    }

    private void cancelarComMulta(Reserva r) {
        LocalDateTime quando = r.getInicio().minusHours(10);
        BigDecimal multa = PoliticaCancelamento.calcularMulta(r.getValorTotal(), r.getInicio(), quando);
        r.cancelar(quando, multa);
        reservas.salvar(r);
        todasReservas.remove(r);
        if (r.getCobrancaId() == null) {
            return;
        }
        cobrancas.buscarPorId(r.getCobrancaId()).ifPresent(c -> {
            c.estornar(quando);
            cobrancas.salvar(c);
            Cobranca m = new Cobranca(r.getMembroId(), TipoCobranca.MULTA, r.getId(),
                    "Multa de 50% (cancelamento com menos de 24h): " + r.getEspacoNome(), multa, quando);
            m.marcarComoPaga(null, quando);
            cobrancas.salvar(m);
        });
    }

    private void criarVisitas(Membro ana, Membro bruno, Membro carla, Membro elisa, Membro felipe) {
        String[][] pessoas = {
                {"Carlos Lima", "123.456.789-09"}, {"Júlia Prado", "MG-12.345.678"}, {"Marcos Reis", "987.654.321-00"},
                {"Beatriz Alves", "12.345.678-9"}, {"Rafael Nunes", "456.789.123-45"}, {"Paula Freitas", "SP-98.765.432"},
                {"Tiago Moura", "321.654.987-01"}, {"Larissa Gomes", "CE-20.304.050"}
        };
        List<Visitante> lista = new ArrayList<>();
        for (String[] p : pessoas) {
            lista.add(visitantes.salvar(new Visitante(p[0], p[1])));
        }
        List<Membro> anfitrioes = List.of(ana, bruno, carla, elisa, felipe);
        for (int i = 0; i < 26; i++) {
            Membro m = anfitrioes.get(aleatorio.nextInt(anfitrioes.size()));
            Visitante v = lista.get(aleatorio.nextInt(lista.size()));
            LocalDate dia = hoje.minusDays(1 + aleatorio.nextInt(29));
            if (!m.temPlanoAtivo(dia)) {
                continue;
            }
            LocalDateTime entrada = dia.atTime(9 + aleatorio.nextInt(8), aleatorio.nextInt(4) * 15);
            Visita visita = new Visita(v.getId(), v.getNome(), v.getDocumento(), m.getId(), m.getNome(), entrada);
            visita.registrarSaida(entrada.plusMinutes(30 + aleatorio.nextInt(180)));
            visitas.salvar(visita);
        }
        visitas.salvar(new Visita(lista.get(0).getId(), lista.get(0).getNome(), lista.get(0).getDocumento(),
                ana.getId(), ana.getNome(), agora.minusMinutes(95)));
        visitas.salvar(new Visita(lista.get(4).getId(), lista.get(4).getNome(), lista.get(4).getDocumento(),
                bruno.getId(), bruno.getNome(), agora.minusMinutes(40)));
    }

    private void notificar(Membro m, String texto) {
        notificacoes.salvar(new Notificacao(m.getId(), texto, agora.minusMinutes(aleatorio.nextInt(600))));
    }
}
