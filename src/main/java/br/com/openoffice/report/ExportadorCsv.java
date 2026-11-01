package br.com.openoffice.report;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Exporta em CSV separado por ";" (abre direto no Excel em português). */
@Component
public class ExportadorCsv implements ExportadorRelatorio {

    private static final String BOM = "﻿";

    @Override
    public String getFormato() {
        return "csv";
    }

    @Override
    public String getTipoConteudo() {
        return "text/csv; charset=UTF-8";
    }

    @Override
    public String getExtensao() {
        return "csv";
    }

    @Override
    public byte[] exportar(RelatorioTabela relatorio) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(campo(relatorio.getTitulo())).append('\n');
        sb.append(campo(relatorio.getSubtitulo())).append('\n').append('\n');
        sb.append(linha(relatorio.getColunas()));
        relatorio.getLinhas().forEach(l -> sb.append(linha(l)));
        if (!relatorio.getResumo().isEmpty()) {
            sb.append('\n');
            relatorio.getResumo().forEach((k, v) -> sb.append(campo(k)).append(';').append(campo(v)).append('\n'));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String linha(List<String> valores) {
        return String.join(";", valores.stream().map(this::campo).toList()) + "\n";
    }

    private String campo(String valor) {
        String v = valor == null ? "" : valor;
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
