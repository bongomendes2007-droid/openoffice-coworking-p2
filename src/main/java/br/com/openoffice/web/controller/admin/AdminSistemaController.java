package br.com.openoffice.web.controller.admin;

import br.com.openoffice.integration.GoogleDriveService;
import br.com.openoffice.repository.ArmazenamentoJson;
import br.com.openoffice.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;

/** Backup, sincronização com Google Drive e log de auditoria. */
@Controller
@RequestMapping("/admin/sistema")
public class AdminSistemaController {

    private final GoogleDriveService drive;
    private final ArmazenamentoJson armazenamento;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public AdminSistemaController(GoogleDriveService drive, ArmazenamentoJson armazenamento,
                                  AuditoriaService auditoria, Clock relogio) {
        this.drive = drive;
        this.armazenamento = armazenamento;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("driveConfigurado", drive.isConfigurado());
        model.addAttribute("driveSituacao", drive.getSituacao());
        model.addAttribute("pastaDrive", drive.getNomePasta());
        model.addAttribute("pastaDados", armazenamento.getDiretorio());
        model.addAttribute("avisos", armazenamento.getAvisos());
        model.addAttribute("auditoria", auditoria.ultimas(150));
        return "admin/sistema";
    }

    @PostMapping("/backup")
    public String backup(RedirectAttributes redirect) {
        Path destino = armazenamento.criarBackup(LocalDateTime.now(relogio));
        auditoria.registrar("admin", "Backup manual criado");
        redirect.addFlashAttribute(destino == null ? "erro" : "sucesso",
                destino == null ? "Não há dados para copiar." : "Backup criado em " + destino + ".");
        return "redirect:/admin/sistema";
    }

    @PostMapping("/drive/enviar")
    public String enviar(RedirectAttributes redirect) {
        int n = drive.enviar();
        redirect.addFlashAttribute("sucesso", n + " arquivo(s) enviados para a pasta \"" + drive.getNomePasta()
                + "\" no Google Drive.");
        return "redirect:/admin/sistema";
    }

    @PostMapping("/drive/baixar")
    public String baixar(RedirectAttributes redirect) {
        int n = drive.baixar();
        redirect.addFlashAttribute("sucesso", n + " arquivo(s) baixados do Google Drive. Os dados foram recarregados "
                + "(um backup dos dados anteriores foi criado).");
        return "redirect:/admin/sistema";
    }
}
