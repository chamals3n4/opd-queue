package lk.opdqueue.controller;

import jakarta.validation.Valid;
import lk.opdqueue.dto.request.RegisterPatientRequest;
import lk.opdqueue.dto.request.UpdatePatientRequest;
import lk.opdqueue.model.Patient;
import lk.opdqueue.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/register")
    public ResponseEntity<Patient> register(@Valid @RequestBody RegisterPatientRequest request) {
        return ResponseEntity.ok(patientService.register(request));
    }

    @GetMapping("/{nic}")
    public ResponseEntity<Patient> findByNic(@PathVariable String nic) {
        return ResponseEntity.ok(patientService.findByNic(nic));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> update(@PathVariable UUID id, @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}