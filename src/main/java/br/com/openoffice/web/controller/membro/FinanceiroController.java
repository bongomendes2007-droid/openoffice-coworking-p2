package br.com.openoffice.web.controller.membro;

import br.com.openoffice.enums.TipoCobranca;
import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.financeiro.Cobranca;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.service.FinanceiroService;
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

/** Histórico financeiro e pagamento de cobranças pendentes. */
@Controller
@RequestMapping("/membro/pagamentos")
public class FinanceiroController {

    private final Sessao sessao;
    private final FinanceiroService financeiro;
    private final EventoService eventos;

    public FinanceiroController(Sessao sessao, FinanceiroService financeiro, EventoService eventos) {
        this.sessao = sessao;
        this.financeiro = financeiro;
        this.eventos = eventos;
    }

    @GetMapping
    public String historico(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        model.addAttribute("cobrancas", financeiro.historico(membro.getId()));
        model.addAttribute("pendentes", financeiro.pendentes(membro.getId()));
        model.addAttribute("totalPago", financeiro.totalPago(membro.getId()));
        model.addAttribute("financeiro", financeiro);
        return "membro/pagamentos";
    }

    @GetMapping("/{id}/pagar")
    public String pagina(@PathVariable String id, HttpSession session, Model model) {
        model.addAttribute("form", new PagamentoForm());
        return preparar(id, session, model);
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable String id, @ModelAttribute("form") PagamentoForm form, HttpSession session,
                        Model model, RedirectAttributes redirect) {
        Membro membro = sessao.membro(session);
        try {
            Cobranca c = financeiro.pagar(membro, id, form.getPagamento());
            if (c.getTipo() == TipoCobranca.EVENTO) {
                eventos.confirmarPagamentoInscricao(c);
            }
            redirect.addFlashAttribute("sucesso", "Pagamento de " + Formatador.moeda(c.getValor()) + " aprovado.");
            return "redirect:/membro/pagamentos";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return preparar(id, session, model);
        }
    }

    private String preparar(String id, HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        Cobranca cobranca = financeiro.buscar(id);
        if (!cobranca.getMembroId().equals(membro.getId())) {
            throw new AcessoNegadoException("Esta cobrança pertence a outro membro.");
        }
        model.addAttribute("cobranca", cobranca);
        return "membro/pagar";
    }
}
