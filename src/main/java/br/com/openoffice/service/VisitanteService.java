package br.com.openoffice.service;

import br.com.openoffice.exception.AcessoNegadoException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.LimiteVisitantesException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.exception.RecursoNaoEncontradoException;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.model.pessoa.Visitante;
import br.com.openoffice.model.visita.Visita;
import br.com.openoffice.regra.RegrasCoworking;
import br.com.openoffice.repository.VisitaRepository;
import br.com.openoffice.repository.VisitanteRepository;
import br.com.openoffice.util.Validador;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Controle de visitantes: só membros com plano ativo autorizam visitas,
 * no máximo 3 visitantes ao mesmo tempo por membro, com histórico de entradas e saídas.
 */
@Service
public class VisitanteService {

    private final VisitanteRepository visitantes;
    private final VisitaRepository visitas;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public VisitanteService(VisitanteRepository visitantes, VisitaRepository visitas, AuditoriaService auditoria,
                            Clock relogio) {
        this.visitantes = visitantes;
        this.visitas = visitas;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public Visita registrarEntrada(Membro membro, String nome, String documento) {
        LocalDate hoje = LocalDate.now(relogio);
        if (!membro.temPlanoAtivo(hoje)) {
            String venceu = membro.getFimDaCobertura()
                    .map(d -> " Seu plano venceu em " + d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".")
                    .orElse("");
            throw new PlanoInativoException("Só membros com plano ativo podem autorizar visitantes." + venceu);
        }
        Validador.obrigatorio(nome, "nome do visitante");
        if (Validador.normalizarDocumento(documento).length() < 5) {
            throw new DadosInvalidosException("Informe um documento válido (CPF ou RG) do visitante.");
        }
        List<Visita> ativas = visitas.emAndamentoDoMembro(membro.getId());
        if (ativas.size() >= RegrasCoworking.MAX_VISITANTES_SIMULTANEOS) {
            throw new LimiteVisitantesException("Limite atingido: cada membro pode ter até "
                    + RegrasCoworking.MAX_VISITANTES_SIMULTANEOS + " visitantes ao mesmo tempo. "
                    + "Registre a saída de um deles antes.");
        }
        Visitante visitante = visitantes.buscarPorDocumento(documento)
                .orElseGet(() -> visitantes.salvar(new Visitante(nome, documento)));
        visitas.emAndamento().stream()
                .filter(v -> v.getVisitanteId().equals(visitante.getId()))
                .findFirst()
                .ifPresent(v -> {
                    throw new DadosInvalidosException(visitante.getNome() + " já está no espaço (autorizado(a) por "
                            + v.getMembroNome() + ").");
                });
        Visita visita = visitas.salvar(new Visita(visitante.getId(), visitante.getNome(), visitante.getDocumento(),
                membro.getId(), membro.getNome(), LocalDateTime.now(relogio)));
        auditoria.registrar(membro.getEmail(), "Entrada do visitante " + visitante.getNome());
        return visita;
    }

    public Visita registrarSaida(Membro membro, String visitaId) {
        Visita visita = buscar(visitaId);
        if (!visita.getMembroId().equals(membro.getId())) {
            throw new AcessoNegadoException("Este visitante foi autorizado por outro membro.");
        }
        return finalizar(visita, membro.getEmail());
    }

    /** A recepção pode registrar a saída de qualquer visitante. */
    public Visita registrarSaidaPelaRecepcao(String visitaId) {
        return finalizar(buscar(visitaId), "admin");
    }

    private Visita finalizar(Visita visita, String quem) {
        visita.registrarSaida(LocalDateTime.now(relogio));
        visitas.salvar(visita);
        auditoria.registrar(quem, "Saída do visitante " + visita.getVisitanteNome());
        return visita;
    }

    public Visita buscar(String id) {
        return visitas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visita " + id + " não encontrada."));
    }

    public List<Visita> ativasDoMembro(String membroId) {
        return visitas.emAndamentoDoMembro(membroId);
    }

    public List<Visita> historicoDoMembro(String membroId) {
        return visitas.doMembro(membroId).stream()
                .sorted(Comparator.comparing(Visita::getEntrada).reversed())
                .toList();
    }

    public List<Visita> todasAtivas() {
        return visitas.emAndamento().stream()
                .sorted(Comparator.comparing(Visita::getEntrada))
                .toList();
    }

    public List<Visita> historico() {
        return visitas.listarTodos().stream()
                .sorted(Comparator.comparing(Visita::getEntrada).reversed())
                .toList();
    }
}
