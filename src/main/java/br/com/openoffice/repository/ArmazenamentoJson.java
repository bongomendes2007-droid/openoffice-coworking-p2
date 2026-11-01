package br.com.openoffice.repository;

import br.com.openoffice.exception.PersistenciaException;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lê e grava listas de objetos em arquivos JSON dentro da pasta de dados.
 *
 * Tratamento de erros:
 * - arquivo ausente: começa com lista vazia e registra um aviso;
 * - arquivo corrompido: move o arquivo para "backup/corrompidos" e começa vazio;
 * - gravação: escreve num arquivo temporário e só depois substitui o original,
 *   para não perder dados se o programa fechar no meio.
 */
@Component
public class ArmazenamentoJson {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoJson.class);
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUPS = 10;

    private final Path diretorio;
    private final ObjectMapper mapper;
    private final List<String> avisos = Collections.synchronizedList(new ArrayList<>());
    /** Na primeira execução (pasta sem nenhum .json) a falta de arquivos é normal e não gera aviso. */
    private final boolean primeiraExecucao;

    public ArmazenamentoJson(@Value("${openoffice.data-dir:data}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath();
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT)
                // Usa os atributos privados, sem exigir getters/setters públicos (mantém o encapsulamento).
                .visibility(PropertyAccessor.ALL, Visibility.NONE)
                .visibility(PropertyAccessor.FIELD, Visibility.ANY)
                .build();
        criarDiretorio(this.diretorio);
        this.primeiraExecucao = !existeAlgumJson();
    }

    private boolean existeAlgumJson() {
        try (Stream<Path> itens = Files.list(diretorio)) {
            return itens.anyMatch(p -> p.toString().endsWith(".json"));
        } catch (IOException e) {
            return false;
        }
    }

    public <T> List<T> ler(String arquivo, Class<T> tipo) {
        Path caminho = diretorio.resolve(arquivo);
        if (!Files.exists(caminho)) {
            if (primeiraExecucao) {
                log.info("Primeira execução: {} será criado.", arquivo);
            } else {
                registrarAviso("Arquivo " + arquivo + " não encontrado. Iniciando com a lista vazia.");
            }
            return new ArrayList<>();
        }
        try {
            JavaType tipoLista = mapper.getTypeFactory().constructCollectionType(List.class, tipo);
            List<T> itens = mapper.readValue(caminho.toFile(), tipoLista);
            return itens == null ? new ArrayList<>() : new ArrayList<>(itens);
        } catch (IOException e) {
            Path destino = moverCorrompido(caminho);
            registrarAviso("Arquivo " + arquivo + " estava corrompido e foi movido para " + destino
                    + ". Iniciando com a lista vazia.");
            log.error("Falha ao ler {}", caminho, e);
            return new ArrayList<>();
        }
    }

    public <T> void gravar(String arquivo, List<T> itens, Class<T> tipo) {
        Path caminho = diretorio.resolve(arquivo);
        Path temporario = diretorio.resolve(arquivo + ".tmp");
        try {
            JavaType tipoLista = mapper.getTypeFactory().constructCollectionType(List.class, tipo);
            mapper.writerFor(tipoLista).writeValue(temporario.toFile(), itens);
            Files.move(temporario, caminho, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível salvar " + arquivo + ": " + e.getMessage(), e);
        }
    }

    /** Copia todos os .json para data/backup/AAAA-MM-DD_HH-MM-SS e mantém só os 10 backups mais recentes. */
    public Path criarBackup(LocalDateTime agora) {
        Path destino = diretorio.resolve("backup").resolve(agora.format(CARIMBO));
        try (Stream<Path> arquivos = Files.list(diretorio)) {
            List<Path> jsons = arquivos.filter(p -> p.toString().endsWith(".json")).toList();
            if (jsons.isEmpty()) {
                return null;
            }
            criarDiretorio(destino);
            for (Path json : jsons) {
                Files.copy(json, destino.resolve(json.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
            limparBackupsAntigos();
            return destino;
        } catch (IOException e) {
            registrarAviso("Não foi possível criar o backup: " + e.getMessage());
            return null;
        }
    }

    private void limparBackupsAntigos() throws IOException {
        Path pasta = diretorio.resolve("backup");
        try (Stream<Path> itens = Files.list(pasta)) {
            List<Path> backups = itens
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().equals("corrompidos"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
            for (Path antigo : backups.subList(Math.min(MAX_BACKUPS, backups.size()), backups.size())) {
                apagarPasta(antigo);
            }
        }
    }

    private void apagarPasta(Path pasta) throws IOException {
        try (Stream<Path> itens = Files.walk(pasta)) {
            for (Path p : itens.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private Path moverCorrompido(Path arquivo) {
        Path pasta = diretorio.resolve("backup").resolve("corrompidos");
        Path destino = pasta.resolve(LocalDateTime.now().format(CARIMBO) + "_" + arquivo.getFileName());
        try {
            criarDiretorio(pasta);
            Files.move(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
            return diretorio.relativize(destino);
        } catch (IOException e) {
            log.error("Não foi possível mover o arquivo corrompido {}", arquivo, e);
            return arquivo.getFileName();
        }
    }

    private void criarDiretorio(Path pasta) {
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível criar a pasta " + pasta, e);
        }
    }

    private void registrarAviso(String aviso) {
        log.warn(aviso);
        avisos.add(aviso);
    }

    /** Avisos gerados ao carregar os arquivos (exibidos no painel do administrador). */
    public List<String> getAvisos() {
        return List.copyOf(avisos);
    }

    public Path getDiretorio() {
        return diretorio;
    }
}
