package br.com.openoffice.web.controller.admin;

import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.repository.ArmazenamentoJson;
import br.com.openoffice.service.AuditoriaService;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.service.FinanceiroService;
import br.com.openoffice.service.MembroService;
import br.com.openoffice.service.ReservaService;
import br.com.openoffice.service.VisitanteService;
import br.com.openoffice.util.Dinheiro;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Painel do administrador: números do dia e do mês, agenda e avisos do sistema. */
@Controller
@RequestMapping("/admin")
public class AdminPainelController {

    private final MembroService membros;
    private final ReservaService reservas;
    private final VisitanteService visitantes;
    private final EventoService eventos;
    private final FinanceiroService financeiro;
    private final AuditoriaService auditoria;
    private final ArmazenamentoJson armazenamento;
    private final Clock relogio;

    public AdminPainelController(MembroService membros, ReservaService reservas, VisitanteService visitantes,
                                 EventoService eventos, FinanceiroService financeiro, AuditoriaService auditoria,
                                 ArmazenamentoJson armazenamento, Clock relogio) {
        this.membros = membros;
        this.reservas = reservas;
        this.visitantes = visitantes;
        this.eventos = eventos;
        this.financeiro = financeiro;
        this.auditoria = auditoria;
        this.armazenamento = armazenamento;
        this.relogio = relogio;
    }

    @GetMapping("/painel")
    public String painel(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model model) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate dia = data == null ? hoje : data;
        List<Membro> todos = membros.listar();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        BigDecimal receitaMes = financeiro.todas().stream()
                .filter(c -> !c.getDataReferencia().isBefore(inicioMes))
                .map(Cobranca::getValorLiquido)
                .reduce(Dinheiro.ZERO, BigDecimal::add);

        model.addAttribute("membrosAtivos", todos.stream().filter(m -> m.temPlanoAtivo(hoje)).count());
        model.addAttribute("membrosVip", todos.stream().filter(m -> m.isVip(hoje)).count());
        model.addAttribute("membrosTotal", todos.size());
        model.addAttribute("reservasHoje", reservas.doDia(hoje).stream().filter(r -> !r.isDeEvento()).count());
        model.addAttribute("visitantesAgora", visitantes.todasAtivas());
        model.addAttribute("receitaMes", receitaMes);
        model.addAttribute("proximosEventos", eventos.proximos().stream().limit(3).toList());
        model.addAttribute("agenda", reservas.agenda(dia, null));
        model.addAttribute("anterior", dia.minusDays(1));
        model.addAttribute("seguinte", dia.plusDays(1));
        model.addAttribute("auditoria", auditoria.ultimas(8));
        model.addAttribute("avisosSistema", armazenamento.getAvisos());
        return "admin/painel";
    }
}
