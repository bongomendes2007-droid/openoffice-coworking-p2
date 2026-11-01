package br.com.openoffice.web.controller;

import br.com.openoffice.exception.AutenticacaoException;
import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.model.pessoa.Usuario;
import br.com.openoffice.service.AutenticacaoService;
import br.com.openoffice.service.MembroService;
import br.com.openoffice.web.form.CadastroForm;
import br.com.openoffice.web.seguranca.Sessao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/** Páginas públicas: login, cadastro e saída. */
@Controller
public class AcessoController {

    private final AutenticacaoService autenticacao;
    private final MembroService membros;
    private final Sessao sessao;

    public AcessoController(AutenticacaoService autenticacao, MembroService membros, Sessao sessao) {
        this.autenticacao = autenticacao;
        this.membros = membros;
        this.sessao = sessao;
    }

    @GetMapping("/")
    public String inicio(HttpSession session) {
        return sessao.usuario(session)
                .map(u -> "redirect:" + u.getPaginaInicial())
                .orElse("redirect:/login");
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        Optional<Usuario> logado = sessao.usuario(session);
        return logado.map(u -> "redirect:" + u.getPaginaInicial()).orElse("login");
    }

    @PostMapping("/login")
    public String entrar(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String senha,
                         HttpServletRequest request, Model model) {
        try {
            Usuario usuario = autenticacao.autenticar(email, senha);
            request.getSession(true);
            request.changeSessionId(); // evita "fixação de sessão"
            sessao.entrar(request.getSession(), usuario);
            return "redirect:" + usuario.getPaginaInicial();
        } catch (AutenticacaoException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @PostMapping("/sair")
    public String sair(HttpSession session) {
        sessao.sair(session);
        return "redirect:/login?saiu";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult validacao, Model model,
                            RedirectAttributes redirect) {
        if (validacao.hasErrors()) {
            return "cadastro";
        }
        try {
            membros.cadastrar(form.getNome(), form.getCpf(), form.getEmail(), form.getTelefone(), form.getSenha());
            redirect.addFlashAttribute("sucesso", "Conta criada! Entre com seu e-mail e senha.");
            return "redirect:/login";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "cadastro";
        }
    }
}
