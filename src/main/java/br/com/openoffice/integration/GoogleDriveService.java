package br.com.openoffice.integration;

import br.com.openoffice.exception.RegraNegocioException;
import br.com.openoffice.repository.ArmazenamentoJson;
import br.com.openoffice.repository.RepositorioJson;
import br.com.openoffice.service.AuditoriaService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Desafio do trabalho: guardar os arquivos de dados no Google Drive.
 *
 * Funciona com OAuth de "aplicativo instalado": na primeira vez o navegador abre para o
 * usuário autorizar o acesso; depois disso o token fica salvo na pasta "tokens".
 * O escopo "drive.file" só dá acesso aos arquivos criados pelo próprio sistema.
 *
 * Configuração (arquivo .env, veja .env.example):
 *   GOOGLE_DRIVE_CREDENTIALS=credentials.json   (baixado do Google Cloud Console)
 *   GOOGLE_DRIVE_FOLDER_NAME=OpenOffice Coworking - dados   (opcional)
 */
@Service
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final JsonFactory JSON = GsonFactory.getDefaultInstance();
    private static final String TIPO_PASTA = "application/vnd.google-apps.folder";
    private static final Path PASTA_TOKENS = Path.of("tokens");

    private final Path credenciais;
    private final String nomePasta;
    private final int portaAutorizacao;
    private final ArmazenamentoJson armazenamento;
    private final List<RepositorioJson<?>> repositorios;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public GoogleDriveService(@Value("${google.drive.credentials:}") String credenciais,
                              @Value("${google.drive.folder-name:OpenOffice Coworking - dados}") String nomePasta,
                              @Value("${google.drive.auth-port:8888}") int portaAutorizacao,
                              ArmazenamentoJson armazenamento, List<RepositorioJson<?>> repositorios,
                              AuditoriaService auditoria, Clock relogio) {
        this.credenciais = credenciais == null || credenciais.isBlank() ? null : Path.of(credenciais);
        this.nomePasta = nomePasta;
        this.portaAutorizacao = portaAutorizacao;
        this.armazenamento = armazenamento;
        this.repositorios = repositorios;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public boolean isConfigurado() {
        return credenciais != null && Files.isRegularFile(credenciais);
    }

    /** Explica o que falta configurar (exibido na tela do administrador). */
    public String getSituacao() {
        if (credenciais == null) {
            return "Não configurado: defina GOOGLE_DRIVE_CREDENTIALS no arquivo .env (veja o README).";
        }
        if (!Files.isRegularFile(credenciais)) {
            return "Arquivo de credenciais não encontrado em " + credenciais.toAbsolutePath() + ".";
        }
        return Files.isDirectory(PASTA_TOKENS)
                ? "Configurado e autorizado. Pasta no Drive: \"" + nomePasta + "\"."
                : "Configurado. Na primeira sincronização o navegador vai abrir para você autorizar o acesso.";
    }

    public String getNomePasta() {
        return nomePasta;
    }

    /** Envia (ou atualiza) todos os arquivos .json e o log de auditoria para a pasta do Drive. */
    public int enviar() {
        try {
            Drive drive = conectar();
            String pastaId = obterOuCriarPasta(drive);
            int enviados = 0;
            for (Path arquivo : arquivosLocais()) {
                String nome = arquivo.getFileName().toString();
                String tipo = nome.endsWith(".json") ? "application/json" : "text/plain";
                FileContent conteudo = new FileContent(tipo, arquivo.toFile());
                String existente = buscarArquivo(drive, pastaId, nome);
                if (existente == null) {
                    File meta = new File().setName(nome).setParents(List.of(pastaId));
                    drive.files().create(meta, conteudo).setFields("id").execute();
                } else {
                    drive.files().update(existente, new File(), conteudo).execute();
                }
                enviados++;
            }
            auditoria.registrar("admin", enviados + " arquivo(s) enviados ao Google Drive");
            return enviados;
        } catch (IOException | GeneralSecurityException e) {
            log.error("Falha ao enviar para o Google Drive", e);
            throw new RegraNegocioException("Não foi possível enviar ao Google Drive: " + e.getMessage());
        }
    }

    /**
     * Baixa os arquivos .json da pasta do Drive, substituindo os locais (é feito um backup antes)
     * e recarrega os repositórios para o sistema já usar os dados novos.
     */
    public int baixar() {
        try {
            Drive drive = conectar();
            String pastaId = obterOuCriarPasta(drive);
            List<File> remotos = drive.files().list()
                    .setQ("'" + pastaId + "' in parents and trashed = false and name contains '.json'")
                    .setFields("files(id, name)")
                    .execute().getFiles();
            if (remotos == null || remotos.isEmpty()) {
                throw new RegraNegocioException("A pasta \"" + nomePasta + "\" no Drive ainda não tem arquivos. "
                        + "Envie os dados primeiro.");
            }
            armazenamento.criarBackup(LocalDateTime.now(relogio));
            remotos = remotos.stream().filter(f -> f.getName().endsWith(".json")).toList();
            for (File remoto : remotos) {
                Path destino = armazenamento.getDiretorio().resolve(Path.of(remoto.getName()).getFileName());
                try (OutputStream saida = Files.newOutputStream(destino)) {
                    drive.files().get(remoto.getId()).executeMediaAndDownloadTo(saida);
                }
            }
            repositorios.forEach(RepositorioJson::recarregar);
            auditoria.registrar("admin", remotos.size() + " arquivo(s) baixados do Google Drive");
            return remotos.size();
        } catch (IOException | GeneralSecurityException e) {
            log.error("Falha ao baixar do Google Drive", e);
            throw new RegraNegocioException("Não foi possível baixar do Google Drive: " + e.getMessage());
        }
    }

    private Drive conectar() throws IOException, GeneralSecurityException {
        if (!isConfigurado()) {
            throw new RegraNegocioException(getSituacao());
        }
        NetHttpTransport transporte = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets segredos;
        try (Reader leitor = Files.newBufferedReader(credenciais, StandardCharsets.UTF_8)) {
            segredos = GoogleClientSecrets.load(JSON, leitor);
        }
        GoogleAuthorizationCodeFlow fluxo = new GoogleAuthorizationCodeFlow.Builder(transporte, JSON, segredos,
                List.of(DriveScopes.DRIVE_FILE))
                .setDataStoreFactory(new FileDataStoreFactory(PASTA_TOKENS.toFile()))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receptor = new LocalServerReceiver.Builder().setPort(portaAutorizacao).build();
        Credential credencial = new AuthorizationCodeInstalledApp(fluxo, receptor).authorize("admin");
        return new Drive.Builder(transporte, JSON, credencial).setApplicationName("OpenOffice Coworking").build();
    }

    private String obterOuCriarPasta(Drive drive) throws IOException {
        List<File> pastas = drive.files().list()
                .setQ("mimeType = '" + TIPO_PASTA + "' and name = '" + escapar(nomePasta) + "' and trashed = false")
                .setFields("files(id)")
                .execute().getFiles();
        if (pastas != null && !pastas.isEmpty()) {
            return pastas.get(0).getId();
        }
        File pasta = new File().setName(nomePasta).setMimeType(TIPO_PASTA);
        return drive.files().create(pasta).setFields("id").execute().getId();
    }

    private String buscarArquivo(Drive drive, String pastaId, String nome) throws IOException {
        List<File> achados = drive.files().list()
                .setQ("'" + pastaId + "' in parents and name = '" + escapar(nome) + "' and trashed = false")
                .setFields("files(id)")
                .execute().getFiles();
        return achados == null || achados.isEmpty() ? null : achados.get(0).getId();
    }

    private List<Path> arquivosLocais() throws IOException {
        try (Stream<Path> itens = Files.list(armazenamento.getDiretorio())) {
            return itens.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".log"))
                    .toList();
        }
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("'", "\\'");
    }
}
