package br.com.openoffice.web.controller.admin;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.service.EspacoService;
import br.com.openoffice.web.form.EspacoForm;
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

/** Cadastro de espaços. */
@Controller
@RequestMapping("/admin/espacos")
public class AdminEspacoController {

    private final EspacoService espacos;

    public AdminEspacoController(EspacoService espacos) {
        this.espacos = espacos;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("espacos", espacos.listarTodos());
        return "admin/espacos";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new EspacoForm());
        model.addAttribute("tipos", TipoEspaco.values());
        return "admin/espaco-form";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("form") EspacoForm form, BindingResult validacao, Model model,
                            RedirectAttributes redirect) {
        model.addAttribute("tipos", TipoEspaco.values());
        if (validacao.hasErrors()) {
            return "admin/espaco-form";
        }
        try {
            Espaco e = espacos.cadastrar(form.getTipo(), form.getNome(), form.getCapacidade(), form.getDescricao());
            redirect.addFlashAttribute("sucesso", e.getNome() + " cadastrado.");
            return "redirect:/admin/espacos";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "admin/espaco-form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable String id, Model model) {
        Espaco e = espacos.buscar(id);
        model.addAttribute("espaco", e);
        model.addAttribute("form", EspacoForm.de(e));
        model.addAttribute("tipos", TipoEspaco.values());
        return "admin/espaco-form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable String id, @Valid @ModelAttribute("form") EspacoForm form,
                            BindingResult validacao, Model model, RedirectAttributes redirect) {
        Espaco espaco = espacos.buscar(id);
        model.addAttribute("espaco", espaco);
        model.addAttribute("tipos", TipoEspaco.values());
        form.setTipo(espaco.getTipo());
        if (validacao.hasErrors()) {
            return "admin/espaco-form";
        }
        try {
            espacos.atualizar(id, form.getNome(), form.getCapacidade(), form.getDescricao());
            redirect.addFlashAttribute("sucesso", "Espaço atualizado.");
            return "redirect:/admin/espacos";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "admin/espaco-form";
        }
    }

    @PostMapping("/{id}/ativo")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes redirect) {
        espacos.alternarAtivo(id);
        Espaco e = espacos.buscar(id);
        redirect.addFlashAttribute("sucesso", e.getNome() + (e.isAtivo() ? " foi ativado." : " foi desativado."));
        return "redirect:/admin/espacos";
    }
}
