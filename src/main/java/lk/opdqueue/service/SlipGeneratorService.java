package lk.opdqueue.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lk.opdqueue.exception.AppException;
import lk.opdqueue.model.QueueTicket;
import lk.opdqueue.repository.QueueTicketRepository;
import lk.opdqueue.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class SlipGeneratorService {

    private final QRCodeGenerator qrCodeGenerator;
    private final QueueTicketRepository ticketRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public SlipGeneratorService(QRCodeGenerator qrCodeGenerator,
                                QueueTicketRepository ticketRepository) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.ticketRepository = ticketRepository;
    }

    public byte[] generatePdf(String ticketNumber) throws Exception {
        QueueTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Ticket not found: " + ticketNumber));
        return buildPdf(ticket);
    }

    private byte[] buildPdf(QueueTicket ticket) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);

        PageSize pageSize = new PageSize(226, 600);
        pdfDoc.setDefaultPageSize(pageSize);

        Document doc = new Document(pdfDoc);
        doc.setMargins(16, 14, 16, 14);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");
        PdfFont oblique = PdfFontFactory.createFont("Helvetica-Oblique");

        SolidLine solidLine = new SolidLine(0.5f);
        solidLine.setColor(ColorConstants.BLACK);

        doc.add(new Paragraph("GOVERNMENT HOSPITAL")
                .setFont(bold).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
        doc.add(new Paragraph("Outpatient Department")
                .setFont(oblique).setFontSize(7.5f)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(6));
        doc.add(new LineSeparator(solidLine).setMarginBottom(6));
        doc.add(new Paragraph("QUEUE TICKET")
                .setFont(bold).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        doc.add(new Paragraph(ticket.getTicketNumber())
                .setFont(bold).setFontSize(22)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));

        if (ticket.isEmergency()) {
            doc.add(new Paragraph("! EMERGENCY !")
                    .setFont(bold).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        }

        doc.add(new LineSeparator(solidLine).setMarginBottom(6));

        Table table = new Table(UnitValue.createPercentArray(new float[]{42, 58}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);
        addRow(table, "Patient", ticket.getPatient().getFullName(), bold, regular);
        addRow(table, "Department", ticket.getDepartment().getName(), bold, regular);
        addRow(table, "Position", "#" + ticket.getQueuePosition(), bold, regular);
        addRow(table, "Date", ticket.getIssuedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), bold, regular);
        addRow(table, "Time", ticket.getIssuedAt().format(DateTimeFormatter.ofPattern("hh:mm a")), bold, regular);
        doc.add(table);

        doc.add(new LineSeparator(solidLine).setMarginBottom(8));

        String qrUrl = baseUrl + "/status/" + ticket.getTicketNumber();
        byte[] qrBytes = qrCodeGenerator.generate(qrUrl, 130, 130);
        Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(4);
        doc.add(qrImage);

        doc.add(new Paragraph("Scan to track your queue status")
                .setFont(oblique).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
        doc.add(new LineSeparator(solidLine).setMarginBottom(6));
        doc.add(new Paragraph("Please keep this slip until your appointment is complete.")
                .setFont(oblique).setFontSize(6.5f).setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return out.toByteArray();
    }

    private void addRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(8))
                .setBorder(Border.NO_BORDER).setPaddingTop(2).setPaddingBottom(2));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(8))
                .setBorder(Border.NO_BORDER).setPaddingTop(2).setPaddingBottom(2));
    }
}
