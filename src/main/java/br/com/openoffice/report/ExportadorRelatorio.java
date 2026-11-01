package br.com.openoffice.report;

/**
 * Converte um relatório para um formato de arquivo. Cada formato é uma implementação;
 * para adicionar um formato novo (ex.: Excel) basta criar outra classe, sem mexer nas existentes.
 */
public interface ExportadorRelatorio {

    /** Identificador usado na URL (?formato=pdf). */
    String getFormato();

    String getTipoConteudo();

    String getExtensao();

    byte[] exportar(RelatorioTabela relatorio);
}
