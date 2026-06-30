package lk.opdqueue.controller;

import jakarta.validation.Valid;
import lk.opdqueue.dto.request.CreateAppointmentRequest;
import lk.opdqueue.model.Appointment;
import lk.opdqueue.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<Appointment> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/patient/{nic}")
    public ResponseEntity<List<Appointment>> findByPatient(@PathVariable String nic) {
        return ResponseEntity.ok(appointmentService.findByPatientNic(nic));
    }
}