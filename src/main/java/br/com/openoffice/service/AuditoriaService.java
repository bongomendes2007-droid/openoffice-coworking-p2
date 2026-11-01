package br.com.openoffice.service;

import br.com.openoffice.repository.ArmazenamentoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registra as ações importantes num arquivo de texto (data/auditoria.log). */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Path arquivo;
    private final Clock relogio;

    public AuditoriaService(ArmazenamentoJson armazenamento, Clock relogio) {
        this.arquivo = armazenamento.getDiretorio().resolve("auditoria.log");
        this.relogio = relogio;
    }

    public synchronized void registrar(String quem, String acao) {
        String linha = LocalDateTime.now(relogio).format(FORMATO) + " | " + quem + " | " + acao
                + System.lineSeparator();
        try {
            Files.writeString(arquivo, linha, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // A auditoria nunca deve impedir a operação principal.
            log.warn("Não foi possível gravar a auditoria: {}", e.getMessage());
        }
    }

    /** Últimas linhas do log, da mais recente para a mais antiga. */
    public synchronized List<String> ultimas(int quantidade) {
        if (!Files.exists(arquivo)) {
            return List.of();
        }
        try {
            List<String> linhas = new ArrayList<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
            Collections.reverse(linhas);
            return linhas.subList(0, Math.min(quantidade, linhas.size()));
        } catch (IOException e) {
            return List.of("Não foi possível ler o arquivo de auditoria: " + e.getMessage());
        }
    }
}
