package lk.opdqueue.service;

import lk.opdqueue.dto.request.RegisterPatientRequest;
import lk.opdqueue.dto.request.UpdatePatientRequest;
import lk.opdqueue.entity.Patient;
import lk.opdqueue.exception.PatientNotFoundException;
import lk.opdqueue.factory.PatientFactory;
import lk.opdqueue.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;


@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientFactory patientFactory;

    public PatientService(PatientRepository patientRepository, PatientFactory patientFactory) {
        this.patientRepository = patientRepository;
        this.patientFactory = patientFactory;
    }

    public Patient register(RegisterPatientRequest request) {
        if (patientRepository.existsByNic(request.getNic())) {
            return patientRepository.findByNic(request.getNic()).get();
        }
        Patient patient = patientFactory.create(
                request.getNic(), request.getFullName(),
                request.getDateOfBirth(), request.getGender(),
                request.getContactNumber()
        );
        return patientRepository.save(patient);
    }

    public Patient findByNic(String nic) {
        return patientRepository.findByNic(nic)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with NIC: " + nic));
    }

    public Patient update(UUID id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found"));
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        return patientRepository.save(patient);
    }

    public void delete(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException("Patient not found");
        }
        patientRepository.deleteById(id);
    }
}