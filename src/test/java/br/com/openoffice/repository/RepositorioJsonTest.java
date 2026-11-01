package br.com.openoffice.repository;

import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.espaco.SalaPrivada;
import br.com.openoffice.model.espaco.SalaReuniao;
import br.com.openoffice.model.financeiro.Pagamento;
import br.com.openoffice.model.financeiro.PagamentoCartao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Persistência em JSON: gravação, leitura polimórfica e tratamento de arquivos ausentes/corrompidos. */
class RepositorioJsonTest {

    @TempDir
    Path pasta;

    @Test
    @DisplayName("Salva e lê de volta mantendo a subclasse correta (polimorfismo no JSON)")
    void salvaELePolimorfico() {
        EspacoRepository repo = new EspacoRepository(new ArmazenamentoJson(pasta.toString()));
        repo.salvar(new SalaReuniao("Sala 1", 8, "teste"));
        repo.salvar(new SalaPrivada("Privada", 4, "teste"));

        EspacoRepository relido = new EspacoRepository(new ArmazenamentoJson(pasta.toString()));
        assertThat(relido.listarTodos()).hasSize(2);
        Espaco privada = relido.buscarPorId("E0002").orElseThrow();
        assertThat(privada).isInstanceOf(SalaPrivada.class);
        assertThat(privada.isExclusivoVip()).isTrue();
    }

    @Test
    @DisplayName("Ids são sequenciais e legíveis (E0001, E0002...)")
    void idsSequenciais() {
        EspacoRepository repo = new EspacoRepository(new ArmazenamentoJson(pasta.toString()));
        assertThat(repo.salvar(new SalaReuniao("A", 2, "")).getId()).isEqualTo("E0001");
        assertThat(repo.salvar(new SalaReuniao("B", 2, "")).getId()).isEqualTo("E0002");
    }

    @Test
    @DisplayName("Arquivo ausente: começa vazio e registra um aviso")
    void arquivoAusente() throws Exception {
        Files.writeString(pasta.resolve("outro.json"), "[]"); // não é a primeira execução
        ArmazenamentoJson armazenamento = new ArmazenamentoJson(pasta.toString());
        EspacoRepository repo = new EspacoRepository(armazenamento);
        assertThat(repo.contar()).isZero();
        assertThat(armazenamento.getAvisos()).anyMatch(a -> a.contains("não encontrado"));
    }

    @Test
    @DisplayName("Arquivo corrompido: não quebra, move o arquivo para backup e começa vazio")
    void arquivoCorrompido() throws Exception {
        Files.writeString(pasta.resolve("espacos.json"), "{ isto não é json [[[");
        ArmazenamentoJson armazenamento = new ArmazenamentoJson(pasta.toString());
        EspacoRepository repo = new EspacoRepository(armazenamento);
        assertThat(repo.contar()).isZero();
        assertThat(armazenamento.getAvisos()).anyMatch(a -> a.contains("corrompido"));
        assertThat(Files.list(pasta.resolve("backup").resolve("corrompidos")).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dados completos do cartão não são gravados no arquivo")
    void naoGravaNumeroDoCartao() throws Exception {
        PagamentoRepository repo = new PagamentoRepository(new ArmazenamentoJson(pasta.toString()));
        Pagamento p = new PagamentoCartao("Cliente", "4242 4242 4242 4242", "12/30", "123", 2);
        p.processar("U0001", new BigDecimal("100.00"), LocalDateTime.of(2026, 11, 2, 10, 0));
        repo.salvar(p);
        String json = Files.readString(pasta.resolve("pagamentos.json"));
        assertThat(json).contains("\"finalCartao\" : \"4242\"").doesNotContain("4242424242424242").doesNotContain("\"cvv\"");
    }

    @Test
    @DisplayName("Backup copia os arquivos JSON para uma pasta com data e hora")
    void backup() {
        ArmazenamentoJson armazenamento = new ArmazenamentoJson(pasta.toString());
        new EspacoRepository(armazenamento).salvar(new SalaReuniao("A", 2, ""));
        Path destino = armazenamento.criarBackup(LocalDateTime.of(2026, 11, 2, 10, 0));
        assertThat(destino.resolve("espacos.json")).exists();
    }
}
