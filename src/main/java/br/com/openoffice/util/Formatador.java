package br.com.openoffice.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação no padrão brasileiro. Usado no Java e também nas páginas
 * (nos templates: ${@fmt.moeda(valor)}).
 */
@Component("fmt")
public class Formatador {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEE, dd/MM", BR);

    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        return NumberFormat.getCurrencyInstance(BR).format(valor).replace(' ', ' ');
    }

    public static String data(LocalDate data) {
        return data == null ? "" : data.format(DATA);
    }

    public static String data(LocalDateTime data) {
        return data == null ? "" : data.format(DATA);
    }

    public static String dataHora(LocalDateTime data) {
        return data == null ? "" : data.format(DATA_HORA);
    }

    public static String hora(LocalDateTime data) {
        return data == null ? "" : data.format(HORA);
    }

    public static String diaSemana(LocalDate data) {
        return data == null ? "" : data.format(DIA_SEMANA);
    }

    /** "14:00–16:00" */
    public static String periodo(LocalDateTime inicio, LocalDateTime fim) {
        return hora(inicio) + "–" + hora(fim);
    }

    /** "2h", "1h30", "45min" */
    public static String duracao(Duration d) {
        if (d == null) {
            return "";
        }
        long h = d.toHours();
        long m = d.toMinutesPart();
        if (h == 0) {
            return m + "min";
        }
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
    }

    public static String horas(double horas) {
        return String.format(BR, "%.1fh", horas).replace(",0h", "h");
    }

    public static String percentual(double valor) {
        return String.format(BR, "%.0f%%", valor);
    }
}
