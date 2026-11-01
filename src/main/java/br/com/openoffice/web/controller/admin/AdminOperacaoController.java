package br.com.openoffice.web.controller.admin;

import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.model.visita.Visita;
import br.com.openoffice.repository.UsuarioRepository;
import br.com.openoffice.service.ReservaService;
import br.com.openoffice.service.VisitanteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Operação do dia a dia: todas as reservas e controle de visitantes pela recepção. */
@Controller
@RequestMapping("/admin")
public class AdminOperacaoController {

    private final ReservaService reservas;
    private final VisitanteService visitantes;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public AdminOperacaoController(ReservaService reservas, VisitanteService visitantes, UsuarioRepository usuarios,
                                   Clock relogio) {
        this.reservas = reservas;
        this.visitantes = visitantes;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @GetMapping("/reservas")
    public String reservas(@RequestParam(defaultValue = "proximas") String filtro, Model model) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Reserva> lista = reservas.todas();
        lista = switch (filtro) {
            case "passadas" -> lista.stream().filter(r -> r.isAtiva() && !r.getFim().isAfter(agora)).toList();
            case "canceladas" -> lista.stream().filter(r -> !r.isAtiva()).toList();
            case "todas" -> lista;
            default -> lista.stream().filter(r -> r.isAtiva() && r.getFim().isAfter(agora))
                    .sorted(java.util.Comparator.comparing(Reserva::getInicio)).toList();
        };
        Map<String, String> nomes = new HashMap<>();
        usuarios.listarTodos().forEach(u -> nomes.put(u.getId(), u.getNome()));
        model.addAttribute("reservas", lista);
        model.addAttribute("nomes", nomes);
        model.addAttribute("filtro", filtro);
        return "admin/reservas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelarReserva(@PathVariable String id, @RequestParam(required = false) String motivo,
                                  RedirectAttributes redirect) {
        reservas.cancelarPeloAdministrador(id, motivo);
        redirect.addFlashAttribute("sucesso", "Reserva " + id + " cancelada com reembolso integral. O membro foi avisado.");
        return "redirect:/admin/reservas";
    }

    @GetMapping("/visitas")
    public String visitas(Model model) {
        model.addAttribute("ativas", visitantes.todasAtivas());
        model.addAttribute("historico", visitantes.historico().stream().limit(100).toList());
        return "admin/visitas";
    }

    @PostMapping("/visitas/{id}/saida")
    public String registrarSaida(@PathVariable String id, RedirectAttributes redirect) {
        Visita v = visitantes.registrarSaidaPelaRecepcao(id);
        redirect.addFlashAttribute("sucesso", "Saída de " + v.getVisitanteNome() + " registrada.");
        return "redirect:/admin/visitas";
    }
}
