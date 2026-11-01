package br.com.openoffice.web.controller.admin;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.service.FinanceiroService;
import br.com.openoffice.service.MembroService;
import br.com.openoffice.service.ReservaService;
import br.com.openoffice.service.VisitanteService;
import br.com.openoffice.web.form.CadastroForm;
import br.com.openoffice.web.form.MembroForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Cadastro, edição, desativação e remoção de membros. */
@Controller
@RequestMapping("/admin/membros")
public class AdminMembroController {

    private final MembroService membros;
    private final ReservaService reservas;
    private final FinanceiroService financeiro;
    private final VisitanteService visitantes;
    private final EventoService eventos;
    private final Clock relogio;

    public AdminMembroController(MembroService membros, ReservaService reservas, FinanceiroService financeiro,
                                 VisitanteService visitantes, EventoService eventos, Clock relogio) {
        this.membros = membros;
        this.reservas = reservas;
        this.financeiro = financeiro;
        this.visitantes = visitantes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        List<Membro> lista = membros.listar();
        if (busca != null && !busca.isBlank()) {
            String termo = busca.trim().toLowerCase();
            lista = lista.stream().filter(m -> m.getNome().toLowerCase().contains(termo)
                    || m.getEmail().contains(termo)
                    || (m.getDocumento() != null && m.getDocumento().contains(termo))).toList();
        }
        model.addAttribute("membros", lista);
        model.addAttribute("busca", busca);
        model.addAttribute("hojeData", LocalDate.now(relogio));
        return "admin/membros";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "admin/membro-novo";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult validacao, Model model,
                            RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return "admin/membro-novo";
        }
        try {
            Membro m = membros.cadastrar(form.getNome(), form.getCpf(), form.getEmail(), form.getTelefone(),
                    form.getSenha());
            redirect.addFlashAttribute("sucesso", "Membro " + m.getNome() + " cadastrado.");
            return "redirect:/admin/membros/" + m.getId();
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "admin/membro-novo";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, Model model) {
        Membro m = membros.buscar(id);
        model.addAttribute("membro", m);
        model.addAttribute("hojeData", LocalDate.now(relogio));
        model.addAttribute("reservas", reservas.doMembro(id));
        model.addAttribute("cobrancas", financeiro.historico(id));
        model.addAttribute("totalPago", financeiro.totalPago(id));
        model.addAttribute("visitas", visitantes.historicoDoMembro(id));
        model.addAttribute("eventos", eventos.doMembro(id));
        return "admin/membro-detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        model.addAttribute("membro", membros.buscar(id));
        model.addAttribute("form", MembroForm.de(membros.buscar(id)));
        return "admin/membro-editar";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable String id, @Valid @ModelAttribute("form") MembroForm form,
                            BindingResult validacao, Model model, RedirectAttributes redirect) {
        model.addAttribute("membro", membros.buscar(id));
        if (validacao.hasErrors()) {
            return "admin/membro-editar";
        }
        try {
            membros.atualizar(id, form.getNome(), form.getCpf(), form.getEmail(), form.getTelefone());
            redirect.addFlashAttribute("sucesso", "Dados atualizados.");
            return "redirect:/admin/membros/" + id;
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "admin/membro-editar";
        }
    }

    @PostMapping("/{id}/ativo")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes redirect) {
        membros.alternarAtivo(id);
        Membro m = membros.buscar(id);
        redirect.addFlashAttribute("sucesso", m.getNome() + (m.isAtivo() ? " foi reativado." : " foi desativado."));
        return "redirect:/admin/membros/" + id;
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable String id, RedirectAttributes redirect) {
        String nome = membros.buscar(id).getNome();
        membros.remover(id);
        redirect.addFlashAttribute("sucesso", nome + " foi removido.");
        return "redirect:/admin/membros";
    }
}
