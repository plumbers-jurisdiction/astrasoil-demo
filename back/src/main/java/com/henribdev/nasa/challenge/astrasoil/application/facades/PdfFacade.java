package com.henribdev.nasa.challenge.astrasoil.application.facades;

import java.io.ByteArrayOutputStream;

import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import com.henribdev.nasa.challenge.astrasoil.domain.models.report.ReportTemplate;

// Only class that talks to OpenPDF
@Component
public class PdfFacade {

    public byte[] renderTemplate(ReportTemplate template) {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, output);

        document.open();
        // OpenPDF refuses to close a document with no pages
        writer.setPageEmpty(false);

        if (!template.title().isBlank()) {
            document.add(new Paragraph(template.title()));
        }

        if (!template.headers().isEmpty()) {
            PdfPTable table = new PdfPTable(template.headers().size());
            template.headers().forEach(table::addCell);
            template.rows().forEach(row -> row.forEach(table::addCell));
            document.add(table);
        }

        document.close();

        return output.toByteArray();
    }

}
