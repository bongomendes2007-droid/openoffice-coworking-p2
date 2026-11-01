package br.com.openoffice.web.controller.admin;

import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.report.ExportadorRelatorio;
import br.com.openoffice.report.RelatorioTabela;
import br.com.openoffice.report.TipoRelatorio;
import br.com.openoffice.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Relatórios na tela e exportação para PDF ou CSV. */
@Controller
@RequestMapping("/admin/relatorios")
public class AdminRelatorioController {

    private final RelatorioService relatorios;
    private final List<ExportadorRelatorio> exportadores;
    private final Clock relogio;

    public AdminRelatorioController(RelatorioService relatorios, List<ExportadorRelatorio> exportadores, Clock relogio) {
        this.relatorios = relatorios;
        this.exportadores = exportadores;
        this.relogio = relogio;
    }

    @GetMapping
    public String pagina(@RequestParam(defaultValue = "ocupacao-sala") String tipo,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                         Model model) {
        TipoRelatorio tipoRelatorio = TipoRelatorio.porCodigo(tipo);
        LocalDate fim = ate == null ? LocalDate.now(relogio) : ate;
        LocalDate inicio = de == null ? fim.minusDays(29) : de;
        model.addAttribute("tipos", TipoRelatorio.values());
        model.addAttribute("tipo", tipoRelatorio);
        model.addAttribute("de", inicio);
        model.addAttribute("ate", fim);
        try {
            model.addAttribute("relatorio", relatorios.gerar(tipoRelatorio, inicio, fim));
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
        }
        return "admin/relatorios";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam String tipo, @RequestParam String formato,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        ExportadorRelatorio exportador = exportadores.stream()
                .filter(e -> e.getFormato().equalsIgnoreCase(formato))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Formato \"" + formato + "\" não suportado."));
        TipoRelatorio tipoRelatorio = TipoRelatorio.porCodigo(tipo);
        RelatorioTabela relatorio = relatorios.gerar(tipoRelatorio, de, ate);
        String nome = "relatorio_" + tipoRelatorio.getCodigo() + "_" + de + "_a_" + ate + "." + exportador.getExtensao();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nome, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(exportador.getTipoConteudo()))
                .body(exportador.exportar(relatorio));
    }
}
