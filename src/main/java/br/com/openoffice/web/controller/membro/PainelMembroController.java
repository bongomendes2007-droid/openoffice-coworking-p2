package br.com.openoffice.web.controller.membro;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.AutenticacaoService;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.service.FinanceiroService;
import br.com.openoffice.service.NotificacaoService;
import br.com.openoffice.service.PlanoService;
import br.com.openoffice.service.ReservaService;
import br.com.openoffice.service.VisitanteService;
import br.com.openoffice.web.form.SenhaForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

/** Painel inicial, agenda de ocupação, avisos e perfil do membro. */
@Controller
@RequestMapping("/membro")
public class PainelMembroController {

    private final Sessao sessao;
    private final ReservaService reservas;
    private final VisitanteService visitantes;
    private final EventoService eventos;
    private final FinanceiroService financeiro;
    private final NotificacaoService notificacoes;
    private final PlanoService planos;
    private final AutenticacaoService autenticacao;
    private final Clock relogio;

    public PainelMembroController(Sessao sessao, ReservaService reservas, VisitanteService visitantes,
                                  EventoService eventos, FinanceiroService financeiro, NotificacaoService notificacoes,
                                  PlanoService planos, AutenticacaoService autenticacao, Clock relogio) {
        this.sessao = sessao;
        this.reservas = reservas;
        this.visitantes = visitantes;
        this.eventos = eventos;
        this.financeiro = financeiro;
        this.notificacoes = notificacoes;
        this.planos = planos;
        this.autenticacao = autenticacao;
        this.relogio = relogio;
    }

    @GetMapping("/painel")
    public String painel(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        LocalDate hoje = LocalDate.now(relogio);
        model.addAttribute("membro", membro);
        model.addAttribute("assinatura", membro.getAssinaturaAtiva(hoje).orElse(null));
        model.addAttribute("vip", membro.isVip(hoje));
        model.addAttribute("diasParaVencer", planos.diasParaVencer(membro));
        model.addAttribute("proximasReservas", reservas.proximasDoMembro(membro.getId(), 4));
        model.addAttribute("visitantesAtivos", visitantes.ativasDoMembro(membro.getId()));
        model.addAttribute("meusEventos", eventos.proximosDoMembro(membro.getId()));
        model.addAttribute("pendencias", financeiro.pendentes(membro.getId()));
        model.addAttribute("notificacoes", notificacoes.recentes(membro.getId(), 6));
        return "membro/painel";
    }

    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        LocalDate dia = data == null ? LocalDate.now(relogio) : data;
        model.addAttribute("agenda", reservas.agenda(dia, membro.getId()));
        model.addAttribute("anterior", dia.minusDays(1));
        model.addAttribute("seguinte", dia.plusDays(1));
        return "membro/agenda";
    }

    @PostMapping("/avisos/lidos")
    public String marcarAvisosLidos(HttpSession session) {
        notificacoes.marcarTodasComoLidas(sessao.membro(session).getId());
        return "redirect:/membro/painel";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        model.addAttribute("membro", sessao.membro(session));
        model.addAttribute("senhaForm", new SenhaForm());
        return "membro/perfil";
    }

    @PostMapping("/perfil/senha")
    public String alterarSenha(@Valid @ModelAttribute("senhaForm") SenhaForm form, BindingResult validacao,
                               HttpSession session, Model model, RedirectAttributes redirect) {
        Membro membro = sessao.membro(session);
        if (!validacao.hasErrors()) {
            try {
                autenticacao.alterarSenha(membro, form.getSenhaAtual(), form.getNovaSenha());
                redirect.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
                return "redirect:/membro/perfil";
            } catch (RegraNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        }
        model.addAttribute("membro", membro);
        return "membro/perfil";
    }
}
