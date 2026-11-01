package br.com.openoffice.model.evento;

import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.Entidade;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.regra.RegrasCoworking;
import br.com.openoffice.util.Validador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Evento ou workshop. Controla vagas, inscrições, lista de espera e o período
 * em que só membros VIP podem se inscrever.
 */
public class Evento extends Entidade {

    private String titulo;
    private String descricao;
    private String espacoId;
    private String espacoNome;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private int capacidade;
    private BigDecimal preco;
    private boolean especial;
    private LocalDateTime aberturaGeral;
    private LocalDateTime criadoEm;
    private String reservaId;
    private final List<Inscricao> inscricoes = new ArrayList<>();
    private final List<String> listaEspera = new ArrayList<>();

    protected Evento() {
    }

    public Evento(String titulo, String descricao, String espacoId, String espacoNome, LocalDateTime inicio,
                  LocalDateTime fim, int capacidade, BigDecimal preco, boolean especial,
                  LocalDateTime aberturaGeral, LocalDateTime criadoEm) {
        this.titulo = Validador.obrigatorio(titulo, "título");
        this.descricao = descricao == null ? "" : descricao.trim();
        if (!fim.isAfter(inicio)) {
            throw new DadosInvalidosException("O fim do evento precisa ser depois do início.");
        }
        if (capacidade < 1) {
            throw new DadosInvalidosException("O evento precisa ter pelo menos 1 vaga.");
        }
        if (preco == null || preco.signum() < 0) {
            throw new DadosInvalidosException("O preço do evento não pode ser negativo.");
        }
        this.espacoId = espacoId;
        this.espacoNome = espacoNome;
        this.inicio = inicio;
        this.fim = fim;
        this.capacidade = capacidade;
        this.preco = preco;
        this.especial = especial;
        this.aberturaGeral = aberturaGeral;
        this.criadoEm = criadoEm;
    }

    /** VIP tem 15% de desconto apenas em eventos especiais. */
    public Orcamento calcularValor(boolean membroVip) {
        int desconto = especial && membroVip ? RegrasCoworking.DESCONTO_VIP_EVENTO_ESPECIAL : 0;
        return Orcamento.de(preco, desconto);
    }

    public int getVagasOcupadas() {
        return (int) inscricoes.stream().filter(Inscricao::ocupaVaga).count();
    }

    public int getVagasRestantes() {
        return Math.max(0, capacidade - getVagasOcupadas());
    }

    public boolean isLotado() {
        return getVagasRestantes() == 0;
    }

    /** Antes da abertura geral, só membros VIP podem se inscrever. */
    public boolean isAbertoParaTodos(LocalDateTime agora) {
        return !agora.isBefore(aberturaGeral);
    }

    public boolean isInscricoesEncerradas(LocalDateTime agora) {
        return !agora.isBefore(inicio);
    }

    public boolean isRealizado(LocalDateTime agora) {
        return !fim.isAfter(agora);
    }

    public Optional<Inscricao> inscricaoAtivaDe(String membroId) {
        return inscricoes.stream().filter(i -> i.ocupaVaga() && i.getMembroId().equals(membroId)).findFirst();
    }

    public void adicionarInscricao(Inscricao inscricao) {
        inscricoes.add(inscricao);
        listaEspera.remove(inscricao.getMembroId());
    }

    public boolean estaNaListaEspera(String membroId) {
        return listaEspera.contains(membroId);
    }

    /** Posição na lista de espera começando em 1 (0 se não estiver na lista). */
    public int posicaoNaListaEspera(String membroId) {
        return listaEspera.indexOf(membroId) + 1;
    }

    public void entrarNaListaEspera(String membroId) {
        if (!listaEspera.contains(membroId)) {
            listaEspera.add(membroId);
        }
    }

    public void sairDaListaEspera(String membroId) {
        listaEspera.remove(membroId);
    }

    /** Retira e devolve o primeiro da fila, se houver. */
    public Optional<String> chamarProximoDaEspera() {
        return listaEspera.isEmpty() ? Optional.empty() : Optional.of(listaEspera.remove(0));
    }

    public void vincularReserva(String reservaId) {
        this.reservaId = reservaId;
    }

    public List<Inscricao> getInscricoes() {
        return Collections.unmodifiableList(inscricoes);
    }

    public List<Inscricao> getInscricoesAtivas() {
        return inscricoes.stream().filter(Inscricao::ocupaVaga).toList();
    }

    public List<String> getListaEspera() {
        return Collections.unmodifiableList(listaEspera);
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEspacoId() {
        return espacoId;
    }

    public String getEspacoNome() {
        return espacoNome;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public boolean isEspecial() {
        return especial;
    }

    public LocalDateTime getAberturaGeral() {
        return aberturaGeral;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getReservaId() {
        return reservaId;
    }
}
