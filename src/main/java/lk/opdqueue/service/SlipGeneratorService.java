package lk.opdqueue.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageDataFactory;
import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.exception.TicketNotFoundException;
import lk.opdqueue.repository.QueueTicketRepository;
import lk.opdqueue.util.QRCodeGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class SlipGeneratorService {

    private final QRCodeGenerator qrCodeGenerator;
    private final ObjectProvider<R2StorageService> r2StorageServiceProvider;
    private final QueueTicketRepository ticketRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public SlipGeneratorService(QRCodeGenerator qrCodeGenerator,
                                 ObjectProvider<R2StorageService> r2StorageServiceProvider,
                                 QueueTicketRepository ticketRepository) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.r2StorageServiceProvider = r2StorageServiceProvider;
        this.ticketRepository = ticketRepository;
    }

    public String generateAndUpload(QueueTicket ticket) throws Exception {
        byte[] pdfBytes = buildPdf(ticket);
        String key = "slips/" + ticket.getTicketNumber() + ".pdf";
        R2StorageService r2StorageService = r2StorageServiceProvider.getIfAvailable();
        if (r2StorageService != null) {
            return r2StorageService.uploadPdf(key, pdfBytes);
        }
        return baseUrl + "/slips/" + ticket.getTicketNumber() + ".pdf";
    }

    private byte[] buildPdf(QueueTicket ticket) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        // Header
        Paragraph header = new Paragraph("OPD Queue Slip")
                .setFont(bold)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY);
        document.add(header);

        Paragraph hospital = new Paragraph("Government Hospital")
                .setFont(regular)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(hospital);

        document.add(new Paragraph("\n"));

        // Ticket number big
        Paragraph ticketNum = new Paragraph(ticket.getTicketNumber())
                .setFont(bold)
                .setFontSize(32)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.BLUE);
        document.add(ticketNum);

        if (ticket.isEmergency()) {
            Paragraph emergency = new Paragraph("⚠ EMERGENCY")
                    .setFont(bold)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.RED);
            document.add(emergency);
        }

        document.add(new Paragraph("\n"));

        // Details table
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        addRow(table, "Patient", ticket.getPatient().getFullName(), bold, regular);
        addRow(table, "Department", ticket.getDepartment().getName(), bold, regular);
        addRow(table, "Queue Position", String.valueOf(ticket.getQueuePosition()), bold, regular);
        addRow(table, "Est. Wait", ticket.getEstimatedWaitMinutes() + " minutes", bold, regular);
        addRow(table, "Issued At", ticket.getIssuedAt()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), bold, regular);

        document.add(table);
        document.add(new Paragraph("\n"));

        // QR code
        String qrContent = baseUrl + "/status/" + ticket.getTicketNumber();
        byte[] qrBytes = qrCodeGenerator.generate(qrContent, 150, 150);
        Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        document.add(qrImage);

        Paragraph qrNote = new Paragraph("Scan to track your queue status")
                .setFont(regular)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY);
        document.add(qrNote);

        document.close();
        return outputStream.toByteArray();
    }

    private void addRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(10)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(10)));
    }

    public byte[] getPdfBytes(String ticketNumber) throws Exception {
        QueueTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketNumber));
        return buildPdf(ticket);
    }
}