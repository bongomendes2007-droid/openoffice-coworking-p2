package br.com.openoffice.web.controller.membro;

import br.com.openoffice.enums.TipoPlano;
import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.plano.Assinatura;
import br.com.openoffice.service.PlanoService;
import br.com.openoffice.util.Formatador;
import br.com.openoffice.web.form.PagamentoForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

/** Planos de assinatura: ver o plano atual, assinar e renovar. */
@Controller
@RequestMapping("/membro/plano")
public class PlanoController {

    private final Sessao sessao;
    private final PlanoService planos;
    private final Clock relogio;

    public PlanoController(Sessao sessao, PlanoService planos, Clock relogio) {
        this.sessao = sessao;
        this.planos = planos;
        this.relogio = relogio;
    }

    @GetMapping
    public String pagina(@RequestParam(required = false) TipoPlano escolhido, HttpSession session, Model model) {
        PagamentoForm form = new PagamentoForm();
        form.setPlano(escolhido);
        model.addAttribute("form", form);
        return preparar(session, model);
    }

    @PostMapping
    public String assinar(@ModelAttribute("form") PagamentoForm form, HttpSession session, Model model,
                          RedirectAttributes redirect) {
        Membro membro = sessao.membro(session);
        try {
            Assinatura a = planos.assinar(membro, form.getPlano(), form.getPagamento());
            redirect.addFlashAttribute("sucesso", "Plano " + a.getTipo().getDescricao() + " contratado! Válido de "
                    + Formatador.data(a.getInicio()) + " a " + Formatador.data(a.getFim()) + ".");
            return "redirect:/membro/plano";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return preparar(session, model);
        }
    }

    private String preparar(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        LocalDate hoje = LocalDate.now(relogio);
        model.addAttribute("membro", membro);
        model.addAttribute("planos", TipoPlano.values());
        model.addAttribute("assinaturaAtiva", membro.getAssinaturaAtiva(hoje).orElse(null));
        model.addAttribute("inicioProximo", planos.inicioDoProximoPlano(membro));
        model.addAttribute("diasParaVencer", planos.diasParaVencer(membro));
        return "membro/plano";
    }
}
