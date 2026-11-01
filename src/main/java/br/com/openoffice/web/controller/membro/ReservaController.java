package br.com.openoffice.web.controller.membro;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.service.EspacoService;
import br.com.openoffice.service.ReservaService;
import br.com.openoffice.service.dto.ResultadoCancelamento;
import br.com.openoffice.util.Formatador;
import br.com.openoffice.web.form.ReservaForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalTime;

/**
 * Reservas do membro. O fluxo tem dois passos, como na simulação do console:
 * 1) dados da reserva → 2) resumo com valor/desconto e pagamento → confirmação.
 */
@Controller
@RequestMapping("/membro/reservas")
public class ReservaController {

    private final Sessao sessao;
    private final ReservaService reservas;
    private final EspacoService espacos;
    private final Clock relogio;

    public ReservaController(Sessao sessao, ReservaService reservas, EspacoService espacos, Clock relogio) {
        this.sessao = sessao;
        this.reservas = reservas;
        this.espacos = espacos;
        this.relogio = relogio;
    }

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        model.addAttribute("reservas", reservas.doMembro(membro.getId()));
        return "membro/reservas";
    }

    @GetMapping("/nova")
    public String nova(@RequestParam(required = false) String espacoId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       @RequestParam(required = false) Integer hora,
                       HttpSession session, Model model) {
        ReservaForm form = new ReservaForm();
        form.setEspacoId(espacoId);
        form.setData(data == null ? LocalDate.now(relogio) : data);
        if (hora != null && hora >= 0 && hora < 23) {
            form.setInicio(LocalTime.of(hora, 0));
            form.setFim(LocalTime.of(hora + 1, 0));
        }
        model.addAttribute("form", form);
        return prepararFormulario(session, model);
    }

    @PostMapping("/resumo")
    public String resumo(@Valid @ModelAttribute("form") ReservaForm form, BindingResult validacao,
                         HttpSession session, Model model) {
        if (validacao.hasErrors()) {
            return prepararFormulario(session, model);
        }
        Membro membro = sessao.membro(session);
        try {
            Orcamento orcamento = reservas.simular(membro, form.paraSolicitacao());
            model.addAttribute("orcamento", orcamento);
            model.addAttribute("espaco", espacos.buscar(form.getEspacoId()));
            return "membro/reserva-resumo";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return prepararFormulario(session, model);
        }
    }

    @PostMapping("/confirmar")
    public String confirmar(@Valid @ModelAttribute("form") ReservaForm form, BindingResult validacao,
                            HttpSession session, Model model, RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return prepararFormulario(session, model);
        }
        Membro membro = sessao.membro(session);
        try {
            Reserva reserva = reservas.reservar(membro, form.paraSolicitacao(), form.getPagamento());
            redirect.addFlashAttribute("sucesso", "Reserva " + reserva.getId() + " confirmada: "
                    + reserva.getEspacoNome() + " em " + Formatador.dataHora(reserva.getInicio()) + ".");
            return "redirect:/membro/reservas";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            try {
                // Erro no pagamento: volta ao resumo mantendo os dados.
                model.addAttribute("orcamento", reservas.simular(membro, form.paraSolicitacao()));
                model.addAttribute("espaco", espacos.buscar(form.getEspacoId()));
                return "membro/reserva-resumo";
            } catch (RegraNegocioException outra) {
                // O horário deixou de estar disponível entre o resumo e a confirmação.
                model.addAttribute("erro", outra.getMessage());
                return prepararFormulario(session, model);
            }
        }
    }

    @GetMapping("/{id}/cancelar")
    public String confirmarCancelamento(@PathVariable String id, HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        model.addAttribute("reserva", reservas.buscar(id));
        model.addAttribute("previa", reservas.previaCancelamento(membro, id));
        return "membro/reserva-cancelar";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        ResultadoCancelamento r = reservas.cancelar(sessao.membro(session), id);
        redirect.addFlashAttribute("sucesso", "Reserva " + id + " cancelada. "
                + (r.cobraMulta() && r.multa().signum() > 0
                ? "Multa de " + Formatador.moeda(r.multa()) + " retida; reembolso de " + Formatador.moeda(r.reembolso()) + "."
                : "Reembolso integral de " + Formatador.moeda(r.reembolso()) + "."));
        return "redirect:/membro/reservas";
    }

    private String prepararFormulario(HttpSession session, Model model) {
        Membro membro = sessao.membro(session);
        LocalDate hoje = LocalDate.now(relogio);
        model.addAttribute("espacos", espacos.listarAtivos());
        model.addAttribute("vip", membro.isVip(hoje));
        model.addAttribute("temPlano", membro.temPlanoAtivo(hoje));
        model.addAttribute("limiteData", hoje.plusDays(membro.getDiasAntecedenciaMaxima(hoje)));
        return "membro/reserva-nova";
    }
}
