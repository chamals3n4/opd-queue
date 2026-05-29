package lk.opdqueue.controller;

import jakarta.validation.Valid;
import lk.opdqueue.dto.request.IssueTicketRequest;
import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.dto.response.QueueStatusResponse;
import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/issue")
    public ResponseEntity<QueueTicket> issue(@Valid @RequestBody IssueTicketRequest request) throws Exception {
        return ResponseEntity.ok(queueService.issueTicket(request));
    }

    @GetMapping("/status/{ticketNumber}")
    public ResponseEntity<QueueStatusResponse> status(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(queueService.getStatus(ticketNumber));
    }

    @PostMapping("/call-next/{departmentId}")
    public ResponseEntity<QueueTicket> callNext(@PathVariable Long departmentId) {
        return ResponseEntity.ok(queueService.callNext(departmentId));
    }

    @PostMapping("/complete/{ticketNumber}")
    public ResponseEntity<QueueTicket> complete(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(queueService.complete(ticketNumber));
    }

    @PostMapping("/no-show/{ticketNumber}")
    public ResponseEntity<QueueTicket> noShow(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(queueService.markNoShow(ticketNumber));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<QueueTicket>> departmentQueue(@PathVariable Long departmentId) {
        return ResponseEntity.ok(queueService.getDepartmentQueue(departmentId));
    }
}