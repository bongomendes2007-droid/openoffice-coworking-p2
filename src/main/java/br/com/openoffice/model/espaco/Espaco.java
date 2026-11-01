package br.com.openoffice.model.espaco;

import br.com.openoffice.enums.TipoEspaco;
import br.com.openoffice.exception.CapacidadeExcedidaException;
import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.exception.PlanoInativoException;
import br.com.openoffice.model.Entidade;
import br.com.openoffice.model.financeiro.Orcamento;
import br.com.openoffice.model.pessoa.Membro;
import br.com.openoffice.util.Validador;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Abstração de um espaço reservável. As subclasses definem preço, regras de acesso e
 * descontos; o restante do sistema trata todos os espaços da mesma forma (polimorfismo).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EstacaoTrabalho.class, name = "ESTACAO"),
        @JsonSubTypes.Type(value = SalaReuniao.class, name = "SALA_REUNIAO"),
        @JsonSubTypes.Type(value = SalaPrivada.class, name = "SALA_PRIVADA"),
        @JsonSubTypes.Type(value = Auditorio.class, name = "AUDITORIO")
})
public abstract class Espaco extends Entidade {

    private String nome;
    private int capacidade;
    private String descricao;
    private boolean ativo = true;

    protected Espaco() {
    }

    protected Espaco(String nome, int capacidade, String descricao) {
        setNome(nome);
        setCapacidade(capacidade);
        setDescricao(descricao);
    }

    /** Tipo do espaço (cada subclasse informa o seu). */
    public abstract TipoEspaco getTipo();

    /** Preço por hora definido pelo coworking para este tipo de espaço. */
    public abstract BigDecimal getPrecoHora();

    /**
     * Percentual de desconto que o membro recebe neste espaço.
     * Por padrão não há desconto; salas de reunião e auditório sobrescrevem para dar 20% ao VIP.
     */
    protected int percentualDesconto(boolean membroVip) {
        return 0;
    }

    /**
     * Verifica se o membro pode usar este espaço. Regra geral: precisa de plano ativo.
     * A sala privada sobrescreve este método para exigir também o plano VIP.
     */
    public void validarAcesso(Membro membro, LocalDate hoje) {
        if (!membro.temPlanoAtivo(hoje)) {
            throw new PlanoInativoException("Você precisa de um plano ativo para reservar espaços. "
                    + "Assine ou renove um plano na página \"Meu plano\".");
        }
    }

    /** Indica se o espaço pode receber eventos e workshops. */
    public boolean aceitaEventos() {
        return false;
    }

    public void validarCapacidade(int pessoas) {
        if (pessoas < 1) {
            throw new DadosInvalidosException("Informe pelo menos 1 participante.");
        }
        if (pessoas > capacidade) {
            throw new CapacidadeExcedidaException("Capacidade excedida: " + nome + " comporta até "
                    + capacidade + (capacidade == 1 ? " pessoa." : " pessoas."));
        }
    }

    /** Template Method: o cálculo é o mesmo para todos, mudando só preço e desconto de cada subclasse. */
    public final Orcamento calcularValor(Duration duracao, boolean membroVip) {
        BigDecimal horas = BigDecimal.valueOf(duracao.toMinutes()).divide(BigDecimal.valueOf(60), 4,
                java.math.RoundingMode.HALF_UP);
        return Orcamento.de(getPrecoHora().multiply(horas), percentualDesconto(membroVip));
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validador.obrigatorio(nome, "nome");
    }

    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        if (capacidade < 1) {
            throw new DadosInvalidosException("A capacidade precisa ser de pelo menos 1 pessoa.");
        }
        this.capacidade = capacidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao == null ? "" : descricao.trim();
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    /** Indica se o espaço é exclusivo de membros VIP. */
    public boolean isExclusivoVip() {
        return false;
    }

    /** Desconto que um VIP recebe neste espaço (usado nas telas). */
    public int getDescontoVip() {
        return percentualDesconto(true);
    }
}
