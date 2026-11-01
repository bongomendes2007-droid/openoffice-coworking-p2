package br.com.openoffice.report;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Gera o relatório em PDF com a biblioteca iText. */
@Component
public class ExportadorPdf implements ExportadorRelatorio {

    private static final Color VERDE = new DeviceRgb(31, 111, 92);
    private static final Color CINZA_CLARO = new DeviceRgb(242, 245, 243);
    private static final Color CINZA_TEXTO = new DeviceRgb(91, 102, 98);
    private static final int COLUNAS_PARA_PAISAGEM = 7;

    @Override
    public String getFormato() {
        return "pdf";
    }

    @Override
    public String getTipoConteudo() {
        return "application/pdf";
    }

    @Override
    public String getExtensao() {
        return "pdf";
    }

    @Override
    public byte[] exportar(RelatorioTabela relatorio) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        boolean paisagem = relatorio.getColunas().size() > COLUNAS_PARA_PAISAGEM;
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(saida));
             Document doc = new Document(pdf, paisagem ? PageSize.A4.rotate() : PageSize.A4)) {
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont negrito = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            doc.setFont(normal).setFontSize(10);
            doc.setMargins(36, 36, 36, 36);

            doc.add(new Paragraph("OpenOffice Coworking").setFont(negrito).setFontSize(11).setFontColor(VERDE)
                    .setMarginBottom(0));
            doc.add(new Paragraph(relatorio.getTitulo()).setFont(negrito).setFontSize(18).setMarginTop(2)
                    .setMarginBottom(0));
            doc.add(new Paragraph(relatorio.getSubtitulo()).setFontColor(CINZA_TEXTO).setMarginBottom(14));

            if (relatorio.isVazio()) {
                doc.add(new Paragraph("Nenhum registro no período selecionado.").setFontColor(CINZA_TEXTO));
            } else {
                doc.add(tabela(relatorio, normal, negrito, paisagem));
            }

            if (!relatorio.getResumo().isEmpty()) {
                doc.add(new Paragraph("Resumo").setFont(negrito).setFontSize(12).setMarginTop(16));
                Table resumo = new Table(UnitValue.createPercentArray(new float[]{3, 2})).setWidth(UnitValue
                        .createPercentValue(60));
                relatorio.getResumo().forEach((rotulo, valor) -> {
                    resumo.addCell(celula(rotulo, normal, null).setFontColor(CINZA_TEXTO));
                    resumo.addCell(celula(valor, negrito, null).setTextAlignment(TextAlignment.RIGHT));
                });
                doc.add(resumo);
            }

            doc.add(new Paragraph("Gerado em " + relatorio.getGeradoEm()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")))
                    .setFontSize(8).setFontColor(CINZA_TEXTO).setMarginTop(20));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar o PDF", e);
        }
        return saida.toByteArray();
    }

    private Table tabela(RelatorioTabela relatorio, PdfFont normal, PdfFont negrito, boolean paisagem) {
        Table tabela = new Table(UnitValue.createPercentArray(relatorio.getColunas().size())).useAllAvailableWidth();
        tabela.setFontSize(paisagem ? 8 : 9);
        for (String coluna : relatorio.getColunas()) {
            tabela.addHeaderCell(celula(coluna, negrito, VERDE).setFontColor(DeviceRgb.WHITE));
        }
        List<List<String>> linhas = relatorio.getLinhas();
        for (int i = 0; i < linhas.size(); i++) {
            Color fundo = i % 2 == 1 ? CINZA_CLARO : null;
            for (String valor : linhas.get(i)) {
                tabela.addCell(celula(valor, normal, fundo));
            }
        }
        return tabela;
    }

    private Cell celula(String texto, PdfFont fonte, Color fundo) {
        Cell c = new Cell().add(new Paragraph(texto == null ? "" : texto).setFont(fonte))
                .setBorder(new SolidBorder(new DeviceRgb(217, 221, 216), 0.5f))
                .setPadding(4);
        if (fundo != null) {
            c.setBackgroundColor(fundo);
        }
        return c;
    }
}
