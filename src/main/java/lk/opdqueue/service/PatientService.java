package lk.opdqueue.service;

import lk.opdqueue.dto.request.RegisterPatientRequest;
import lk.opdqueue.dto.request.UpdatePatientRequest;
import lk.opdqueue.exception.AppException;
import lk.opdqueue.model.Patient;
import lk.opdqueue.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient register(RegisterPatientRequest request) {
        if (patientRepository.existsByNic(request.getNic())) {
            return patientRepository.findByNic(request.getNic()).get();
        }
        Patient patient = new Patient();
        patient.setNic(request.getNic());
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        return patientRepository.save(patient);
    }

    public Patient findByNic(String nic) {
        return patientRepository.findByNic(nic)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Patient not found with NIC: " + nic));
    }

    public Patient update(UUID id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Patient not found"));
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        return patientRepository.save(patient);
    }

    public void delete(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        patientRepository.deleteById(id);
    }
}
