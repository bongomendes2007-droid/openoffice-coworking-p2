package br.com.openoffice.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dados do formulário de criação de evento. */
public record NovoEvento(String titulo, String descricao, String espacoId, LocalDateTime inicio, LocalDateTime fim,
                         int capacidade, BigDecimal preco, boolean especial, int diasPrioridadeVip) {
}
