package br.com.openoffice.web.controller.admin;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.evento.Evento;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.service.EspacoService;
import br.com.openoffice.service.EventoService;
import br.com.openoffice.web.form.EventoForm;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Criação e acompanhamento de eventos. */
@Controller
@RequestMapping("/admin/eventos")
public class AdminEventoController {

    private final EventoService eventos;
    private final EspacoService espacos;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public AdminEventoController(EventoService eventos, EspacoService espacos, UsuarioRepository usuarios,
                                 Clock relogio) {
        this.eventos = eventos;
        this.espacos = espacos;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("eventos", eventos.todos());
        return "admin/eventos";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        EventoForm form = new EventoForm();
        form.setData(LocalDate.now(relogio).plusDays(7));
        form.setInicio(LocalTime.of(19, 0));
        form.setFim(LocalTime.of(21, 0));
        model.addAttribute("form", form);
        model.addAttribute("espacos", espacos.listarParaEventos());
        return "admin/evento-form";
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("form") EventoForm form, BindingResult validacao, Model model,
                        RedirectAttributes redirect) {
        model.addAttribute("espacos", espacos.listarParaEventos());
        if (validacao.hasErrors()) {
            return "admin/evento-form";
        }
        try {
            Evento e = eventos.criar(form.paraNovoEvento());
            redirect.addFlashAttribute("sucesso", "Evento \"" + e.getTitulo() + "\" criado e espaço reservado.");
            return "redirect:/admin/eventos/" + e.getId();
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "admin/evento-form";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, Model model) {
        Evento evento = eventos.buscar(id);
        Map<String, String> espera = new LinkedHashMap<>();
        evento.getListaEspera().forEach(mid -> espera.put(mid,
                usuarios.buscarPorId(mid).map(u -> u.getNome()).orElse("(removido)")));
        model.addAttribute("evento", evento);
        model.addAttribute("espera", espera);
        return "admin/evento-detalhe";
    }
}
