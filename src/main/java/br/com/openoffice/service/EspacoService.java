package br.com.openoffice.service;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.model.espaco.Espaco;
import br.com.openoffice.model.reserva.Reserva;
import br.com.openoffice.repository.EspacoRepository;
import br.com.openoffice.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Cadastro de espaços (salas, estações e auditório). */
@Service
public class EspacoService {

    private final EspacoRepository espacos;
    private final ReservaRepository reservas;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public EspacoService(EspacoRepository espacos, ReservaRepository reservas, AuditoriaService auditoria,
                         Clock relogio) {
        this.espacos = espacos;
        this.reservas = reservas;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public Espaco cadastrar(TipoEspaco tipo, String nome, int capacidade, String descricao) {
        if (tipo == null) {
            throw new DadosInvalidosException("Escolha o tipo do espaço.");
        }
        validarNomeUnico(nome, null);
        Espaco espaco = espacos.salvar(tipo.criar(nome, capacidade, descricao));
        auditoria.registrar("admin", "Espaço " + espaco.getId() + " cadastrado: " + espaco.getNome());
        return espaco;
    }

    public Espaco atualizar(String id, String nome, int capacidade, String descricao) {
        Espaco espaco = buscar(id);
        validarNomeUnico(nome, id);
        espaco.setNome(nome);
        espaco.setCapacidade(capacidade);
        espaco.setDescricao(descricao);
        espacos.salvar(espaco);
        auditoria.registrar("admin", "Espaço " + id + " atualizado");
        return espaco;
    }

    /** Desativar impede novas reservas, mas mantém o histórico. Não é permitido com reservas futuras. */
    public void alternarAtivo(String id) {
        Espaco espaco = buscar(id);
        if (espaco.isAtivo()) {
            LocalDateTime agora = LocalDateTime.now(relogio);
            long futuras = reservas.filtrar(r -> r.getEspacoId().equals(id) && r.isAtiva()
                    && r.getFim().isAfter(agora)).size();
            if (futuras > 0) {
                throw new DadosInvalidosException(espaco.getNome() + " tem " + futuras
                        + " reserva(s) futura(s). Cancele-as antes de desativar o espaço.");
            }
            espaco.desativar();
        } else {
            espaco.ativar();
        }
        espacos.salvar(espaco);
        auditoria.registrar("admin", "Espaço " + id + (espaco.isAtivo() ? " ativado" : " desativado"));
    }

    public Espaco buscar(String id) {
        return espacos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Espaço " + id + " não encontrado."));
    }

    public Espaco buscarAtivo(String id) {
        Espaco espaco = buscar(id);
        if (!espaco.isAtivo()) {
            throw new DadosInvalidosException(espaco.getNome() + " está desativado para reservas.");
        }
        return espaco;
    }

    public List<Espaco> listarAtivos() {
        return espacos.listarAtivos();
    }

    public List<Espaco> listarTodos() {
        return espacos.listarTodos().stream()
                .sorted(Comparator.comparing(Espaco::getTipo).thenComparing(Espaco::getNome))
                .toList();
    }

    public List<Espaco> listarParaEventos() {
        return listarAtivos().stream().filter(Espaco::aceitaEventos).toList();
    }

    public List<Reserva> reservasDoEspaco(String id) {
        return reservas.filtrar(r -> r.getEspacoId().equals(id));
    }

    private void validarNomeUnico(String nome, String ignorarId) {
        if (nome != null && espacos.existeNome(nome, ignorarId)) {
            throw new DadosInvalidosException("Já existe um espaço com o nome \"" + nome.trim() + "\".");
        }
    }
}
