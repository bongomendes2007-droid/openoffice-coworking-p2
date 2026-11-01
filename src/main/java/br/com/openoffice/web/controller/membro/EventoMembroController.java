package br.com.openoffice.web.controller.membro;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.model.evento.Inscricao;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.service.dto.ResultadoCancelamento;
import br.com.openoffice.util.Formatador;
import br.com.openoffice.web.form.PagamentoForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

/** Eventos e workshops: lista, inscrição, lista de espera e cancelamento. */
@Controller
@RequestMapping("/membro/eventos")
public class EventoMembroController {

    private final Sessao sessao;
    private final EventoService eventos;
    private final Clock relogio;

    public EventoMembroController(Sessao sessao, EventoService eventos, Clock relogio) {
        this.sessao = sessao;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        model.addAttribute("eventos", eventos.proximos());
        model.addAttribute("membro", membro);
        model.addAttribute("vip", membro.isVip(LocalDate.now(relogio)));
        return "membro/eventos";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, HttpSession session, Model model) {
        model.addAttribute("form", new PagamentoForm());
        return prepararDetalhe(id, session, model);
    }

    @PostMapping("/{id}/inscrever")
    public String inscrever(@PathVariable String id, @ModelAttribute("form") PagamentoForm form, HttpSession session,
                            Model model, RedirectAttributes redirect) {
        try {
            Inscricao i = eventos.inscrever(sessao.membro(session), id, form.getPagamento());
            redirect.addFlashAttribute("sucesso", "Inscrição confirmada"
                    + (i.getValor().signum() > 0 ? " (" + Formatador.moeda(i.getValor()) + ")" : "") + ".");
            return "redirect:/membro/eventos/" + id;
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return prepararDetalhe(id, session, model);
        }
    }

    @PostMapping("/{id}/espera")
    public String entrarNaEspera(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        int posicao = eventos.entrarNaListaEspera(sessao.membro(session), id);
        redirect.addFlashAttribute("sucesso", "Você é o " + posicao + "º da lista de espera. "
                + "Avisaremos no seu painel se abrir uma vaga.");
        return "redirect:/membro/eventos/" + id;
    }

    @PostMapping("/{id}/sair-espera")
    public String sairDaEspera(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        eventos.sairDaListaEspera(sessao.membro(session), id);
        redirect.addFlashAttribute("sucesso", "Você saiu da lista de espera.");
        return "redirect:/membro/eventos/" + id;
    }

    @GetMapping("/{id}/cancelar")
    public String confirmarCancelamento(@PathVariable String id, HttpSession session, Model model) {
        model.addAttribute("evento", eventos.buscar(id));
        model.addAttribute("previa", eventos.previaCancelamento(sessao.membro(session), id));
        return "membro/evento-cancelar";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        ResultadoCancelamento r = eventos.cancelarInscricao(sessao.membro(session), id);
        redirect.addFlashAttribute("sucesso", "Inscrição cancelada. "
                + (r.multa().signum() > 0 ? "Multa de " + Formatador.moeda(r.multa()) + " retida; " : "")
                + (r.reembolso().signum() > 0 ? "reembolso de " + Formatador.moeda(r.reembolso()) + "." : ""));
        return "redirect:/membro/eventos";
    }

    private String prepararDetalhe(String id, HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        Evento evento = eventos.buscar(id);
        model.addAttribute("evento", evento);
        model.addAttribute("membro", membro);
        model.addAttribute("vip", membro.isVip(LocalDate.now(relogio)));
        model.addAttribute("orcamento", eventos.simularInscricao(membro, id));
        model.addAttribute("minhaInscricao", evento.inscricaoAtivaDe(membro.getId()).orElse(null));
        model.addAttribute("posicaoEspera", evento.posicaoNaListaEspera(membro.getId()));
        return "membro/evento-detalhe";
    }
}
