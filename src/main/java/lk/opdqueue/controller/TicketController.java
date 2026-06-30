package lk.opdqueue.controller;

import lk.opdqueue.repository.QueueTicketRepository;
import lk.opdqueue.service.SlipGeneratorService;
import lk.opdqueue.util.QRCodeGenerator;
import lk.opdqueue.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final QueueTicketRepository ticketRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final SlipGeneratorService slipGeneratorService;

    @Value("${app.base-url}")
    private String baseUrl;

    public TicketController(QueueTicketRepository ticketRepository,
                            QRCodeGenerator qrCodeGenerator,
                            SlipGeneratorService slipGeneratorService) {
        this.ticketRepository = ticketRepository;
        this.qrCodeGenerator = qrCodeGenerator;
        this.slipGeneratorService = slipGeneratorService;
    }

    @GetMapping("/{ticketNumber}/qr")
    public ResponseEntity<byte[]> getQr(@PathVariable String ticketNumber) throws Exception {
        ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketNumber));
        String qrContent = baseUrl + "/status/" + ticketNumber;
        byte[] qrBytes = qrCodeGenerator.generate(qrContent, 300, 300);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qr-" + ticketNumber + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrBytes);
    }

    @GetMapping("/{ticketNumber}/slip")
    public ResponseEntity<byte[]> getSlip(@PathVariable String ticketNumber) throws Exception {
        byte[] pdfBytes = slipGeneratorService.generatePdf(ticketNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=slip-" + ticketNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
