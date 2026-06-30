package lk.opdqueue.factory;

import lk.opdqueue.entity.Patient;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class PatientFactory {

    public Patient create(String nic, String fullName, LocalDate dateOfBirth,
                          String gender, String contactNumber) {
        Patient patient = new Patient();
        patient.setNic(nic);
        patient.setFullName(fullName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setContactNumber(contactNumber);
        return patient;
    }
}
