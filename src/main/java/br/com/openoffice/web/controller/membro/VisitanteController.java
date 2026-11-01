package br.com.openoffice.web.controller.membro;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.visita.Visita;
import br.com.openoffice.regra.RegrasCoworking;
import br.com.openoffice.service.VisitanteService;
import br.com.openoffice.util.Formatador;
import br.com.openoffice.web.form.VisitanteForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro de entrada/saída de visitantes do membro. */
@Controller
@RequestMapping("/membro/visitantes")
public class VisitanteController {

    private final Sessao sessao;
    private final VisitanteService visitantes;

    public VisitanteController(Sessao sessao, VisitanteService visitantes) {
        this.sessao = sessao;
        this.visitantes = visitantes;
    }

    @GetMapping
    public String pagina(HttpSession session, Model model) {
        model.addAttribute("form", new VisitanteForm());
        return preparar(session, model);
    }

    @PostMapping
    public String registrarEntrada(@Valid @ModelAttribute("form") VisitanteForm form, BindingResult validacao,
                                   HttpSession session, Model model, RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return preparar(session, model);
        }
        try {
            Visita v = visitantes.registrarEntrada(sessao.membro(session), form.getNome(), form.getDocumento());
            redirect.addFlashAttribute("sucesso", "Entrada de " + v.getVisitanteNome() + " registrada às "
                    + Formatador.hora(v.getEntrada()) + ".");
            return "redirect:/membro/visitantes";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return preparar(session, model);
        }
    }

    @PostMapping("/{id}/saida")
    public String registrarSaida(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        Visita v = visitantes.registrarSaida(sessao.membro(session), id);
        redirect.addFlashAttribute("sucesso", "Saída de " + v.getVisitanteNome() + " registrada às "
                + Formatador.hora(v.getSaida()) + ".");
        return "redirect:/membro/visitantes";
    }

    private String preparar(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        model.addAttribute("ativas", visitantes.ativasDoMembro(membro.getId()));
        model.addAttribute("historico", visitantes.historicoDoMembro(membro.getId()));
        model.addAttribute("limite", RegrasCoworking.MAX_VISITANTES_SIMULTANEOS);
        return "membro/visitantes";
    }
}
