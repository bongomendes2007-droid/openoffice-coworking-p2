package br.com.openoffice.model.visita;

import br.com.openoffice.exception.DadosInvalidosException;
import br.com.openoffice.model.Entidade;

import java.time.Duration;
import java.time.LocalDateTime;

/** Registro de entrada e saída de um visitante, autorizado por um membro. */
public class Visita extends Entidade {

    private String visitanteId;
    private String visitanteNome;
    private String visitanteDocumento;
    private String membroId;
    private String membroNome;
    private LocalDateTime entrada;
    private LocalDateTime saida;

    protected Visita() {
    }

    public Visita(String visitanteId, String visitanteNome, String visitanteDocumento,
                  String membroId, String membroNome, LocalDateTime entrada) {
        this.visitanteId = visitanteId;
        this.visitanteNome = visitanteNome;
        this.visitanteDocumento = visitanteDocumento;
        this.membroId = membroId;
        this.membroNome = membroNome;
        this.entrada = entrada;
    }

    public boolean isEmAndamento() {
        return saida == null;
    }

    public void registrarSaida(LocalDateTime agora) {
        if (!isEmAndamento()) {
            throw new DadosInvalidosException("A saída deste visitante já foi registrada.");
        }
        this.saida = agora.isBefore(entrada) ? entrada : agora;
    }

    /** Tempo de permanência (até agora, se ainda estiver no espaço). */
    public Duration getPermanencia(LocalDateTime agora) {
        return Duration.between(entrada, saida == null ? agora : saida);
    }

    public String getVisitanteId() {
        return visitanteId;
    }

    public String getVisitanteNome() {
        return visitanteNome;
    }

    public String getVisitanteDocumento() {
        return visitanteDocumento;
    }

    public String getMembroId() {
        return membroId;
    }

    public String getMembroNome() {
        return membroNome;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSaida() {
        return saida;
    }
}
