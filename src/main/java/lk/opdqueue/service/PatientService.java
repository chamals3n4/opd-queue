package lk.opdqueue.service;

import lk.opdqueue.dto.request.RegisterPatientRequest;
import lk.opdqueue.entity.Patient;
import lk.opdqueue.exception.PatientNotFoundException;
import lk.opdqueue.factory.PatientFactory;
import lk.opdqueue.repository.PatientRepository;
import org.springframework.stereotype.Service;

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
        Patient patient = switch (request.getPatientType()) {
            case WALK_IN -> patientFactory.createWalkIn(
                    request.getNic(), request.getFullName(),
                    request.getDateOfBirth(), request.getGender(),
                    request.getContactNumber()
            );
            case PRE_REGISTERED -> patientFactory.createPreRegistered(
                    request.getNic(), request.getFullName(),
                    request.getDateOfBirth(), request.getGender(),
                    request.getContactNumber()
            );
        };
        return patientRepository.save(patient);
    }

    public Patient findByNic(String nic) {
        return patientRepository.findByNic(nic)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with NIC: " + nic));
    }
}