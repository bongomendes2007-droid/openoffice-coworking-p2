package br.com.openoffice.web.erro;

import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.PersistenciaException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Última barreira contra erros: qualquer exceção que escape de um controller vira uma página
 * amigável ou uma mensagem na tela anterior. O sistema nunca mostra a página de erro padrão.
 */
@ControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class,
            NoHandlerFoundException.class})
    public ModelAndView naoEncontrado(Exception e) {
        String mensagem = e instanceof RecursoNaoEncontradoException ? e.getMessage()
                : "A página que você procurou não existe.";
        return pagina("error/404", HttpStatus.NOT_FOUND, mensagem);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView parametroInvalido(Exception e) {
        return pagina("error/400", HttpStatus.BAD_REQUEST,
                "Algum dado do endereço está em formato inválido (por exemplo, uma data digitada errada).");
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ModelAndView acessoNegado(AcessoNegadoException e) {
        return pagina("error/403", HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** Regra violada fora de um formulário: volta para a página anterior com a mensagem. */
    @ExceptionHandler(RegraNegocioException.class)
    public String regraVioladaSemFormulario(RegraNegocioException e, HttpServletRequest request,
                                            RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", e.getMessage());
        return "redirect:" + paginaAnterior(request.getHeader("Referer"));
    }

    /** Usa só o caminho do "Referer" (nunca outro site), para voltar à página onde o usuário estava. */
    private String paginaAnterior(String referer) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String caminho = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            return uri.getRawQuery() == null ? caminho : caminho + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    @ExceptionHandler(PersistenciaException.class)
    public ModelAndView falhaAoSalvar(PersistenciaException e) {
        log.error("Erro de persistência", e);
        return pagina("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível salvar os dados. Verifique se a pasta \"data\" pode ser gravada. Detalhe: "
                        + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return pagina("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. A operação não foi concluída, mas seus dados estão seguros.");
    }

    private ModelAndView pagina(String view, HttpStatus status, String mensagem) {
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        mav.addObject("mensagem", mensagem);
        return mav;
    }
}
