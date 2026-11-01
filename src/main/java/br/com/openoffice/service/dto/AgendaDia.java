package br.com.openoffice.service.dto;

import br.com.openoffice.model.espaco.Espaco;

import java.time.LocalDate;
import java.util.List;

/** Grade de ocupação de um dia: uma linha por espaço, uma coluna por hora. */
public record AgendaDia(LocalDate data, List<Integer> horas, List<Linha> linhas) {

    public record Linha(Espaco espaco, List<Celula> celulas) {
    }

    /**
     * @param estado livre, ocupado, minha, evento ou passado
     * @param rotulo texto de ajuda exibido ao passar o mouse
     */
    public record Celula(int hora, String estado, String rotulo) {

        public boolean isLivre() {
            return "livre".equals(estado);
        }
    }
}
