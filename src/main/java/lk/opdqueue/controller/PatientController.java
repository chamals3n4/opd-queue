package lk.opdqueue.controller;

import jakarta.validation.Valid;
import lk.opdqueue.dto.request.RegisterPatientRequest;
import lk.opdqueue.entity.Patient;
import lk.opdqueue.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}