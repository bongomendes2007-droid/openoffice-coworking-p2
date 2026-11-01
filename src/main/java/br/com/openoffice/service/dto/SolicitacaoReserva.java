package br.com.openoffice.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Dados que o membro preenche para pedir uma reserva. */
public record SolicitacaoReserva(String espacoId, LocalDate data, LocalTime inicio, LocalTime fim, int participantes) {

    public LocalDateTime inicioCompleto() {
        return data.atTime(inicio);
    }

    public LocalDateTime fimCompleto() {
        return data.atTime(fim);
    }
}
