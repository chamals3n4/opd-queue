package lk.opdqueue.controller;

import lk.opdqueue.model.Patient;
import lk.opdqueue.model.QueueTicket;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.repository.PatientRepository;
import lk.opdqueue.repository.QueueTicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminController {

    private final QueueTicketRepository ticketRepository;
    private final PatientRepository patientRepository;

    public AdminController(QueueTicketRepository ticketRepository, PatientRepository patientRepository) {
        this.ticketRepository = ticketRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : ticketRepository.countGroupByStatus()) {
            counts.put(((TicketStatus) row[0]).name(), (Long) row[1]);
        }
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/tickets/all")
    public ResponseEntity<List<QueueTicket>> getAllTickets() {
        return ResponseEntity.ok(ticketRepository.findAll());
    }

    @GetMapping("/tickets/active")
    public ResponseEntity<List<QueueTicket>> getActiveTickets() {
        return ResponseEntity.ok(
                ticketRepository.findAllByStatusIn(List.of(
                        TicketStatus.WAITING, TicketStatus.CALLED, TicketStatus.IN_PROGRESS
                ))
        );
    }

    @GetMapping("/tickets/department/{deptId}")
    public ResponseEntity<List<QueueTicket>> getByDept(@PathVariable Long deptId) {
        return ResponseEntity.ok(
                ticketRepository.findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        deptId, TicketStatus.WAITING
                )
        );
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientRepository.findAll());
    }
}