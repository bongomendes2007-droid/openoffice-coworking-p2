package br.com.openoffice.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório pronto para ser exibido: título, colunas, linhas, resumo e (opcionalmente) dados de gráfico.
 * A mesma estrutura é usada pela página HTML, pelo PDF e pelo CSV.
 */
public class RelatorioTabela {

    private final String titulo;
    private final String subtitulo;
    private final List<String> colunas;
    private final List<List<String>> linhas = new ArrayList<>();
    private final Map<String, String> resumo = new LinkedHashMap<>();
    private final LocalDateTime geradoEm;
    private GraficoDados grafico;
    private boolean mapaDeCalor;

    public RelatorioTabela(String titulo, String subtitulo, List<String> colunas, LocalDateTime geradoEm) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.colunas = List.copyOf(colunas);
        this.geradoEm = geradoEm;
    }

    public RelatorioTabela adicionarLinha(Object... valores) {
        List<String> linha = new ArrayList<>();
        for (Object v : valores) {
            linha.add(v == null ? "" : v.toString());
        }
        linhas.add(linha);
        return this;
    }

    public RelatorioTabela adicionarResumo(String rotulo, String valor) {
        resumo.put(rotulo, valor);
        return this;
    }

    public RelatorioTabela comGrafico(GraficoDados grafico) {
        this.grafico = grafico;
        return this;
    }

    /** Pinta as células numéricas conforme o valor (usado na ocupação por horário). */
    public RelatorioTabela comoMapaDeCalor() {
        this.mapaDeCalor = true;
        return this;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public List<String> getColunas() {
        return colunas;
    }

    public List<List<String>> getLinhas() {
        return linhas;
    }

    public Map<String, String> getResumo() {
        return resumo;
    }

    public LocalDateTime getGeradoEm() {
        return geradoEm;
    }

    public GraficoDados getGrafico() {
        return grafico;
    }

    public boolean isMapaDeCalor() {
        return mapaDeCalor;
    }

    public boolean isVazio() {
        return linhas.isEmpty();
    }
}
