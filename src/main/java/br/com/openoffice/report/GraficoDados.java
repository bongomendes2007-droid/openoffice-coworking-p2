package br.com.openoffice.report;

import java.util.List;

/** Dados de um gráfico de barras (desenhado na página com Chart.js). */
public record GraficoDados(String titulo, List<String> rotulos, List<Number> valores, String unidade) {
}
