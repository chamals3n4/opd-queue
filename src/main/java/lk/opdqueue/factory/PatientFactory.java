package lk.opdqueue.factory;

import lk.opdqueue.entity.Patient;
import lk.opdqueue.enums.PatientType;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class PatientFactory {

    public Patient createWalkIn(String nic, String fullName, LocalDate dateOfBirth,
                                String gender, String contactNumber) {
        Patient patient = new Patient();
        patient.setNic(nic);
        patient.setFullName(fullName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setContactNumber(contactNumber);
        patient.setPatientType(PatientType.WALK_IN);
        return patient;
    }

    public Patient createPreRegistered(String nic, String fullName, LocalDate dateOfBirth,
                                       String gender, String contactNumber) {
        Patient patient = new Patient();
        patient.setNic(nic);
        patient.setFullName(fullName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setContactNumber(contactNumber);
        patient.setPatientType(PatientType.PRE_REGISTERED);
        return patient;
    }
}