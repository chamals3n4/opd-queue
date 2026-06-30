package lk.opdqueue.service;

import lk.opdqueue.dto.request.CreateAppointmentRequest;
import lk.opdqueue.exception.AppException;
import lk.opdqueue.model.Appointment;
import lk.opdqueue.model.Doctor;
import lk.opdqueue.model.Patient;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.repository.AppointmentRepository;
import lk.opdqueue.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientService patientService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientService patientService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientService = patientService;
    }

    public Appointment create(CreateAppointmentRequest request) {
        Patient patient = patientService.findByNic(request.getNic());
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Doctor not found: " + request.getDoctorId()));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduledTime(request.getScheduledTime());
        appointment.setNotes(request.getNotes());
        appointment.setStatus(TicketStatus.REGISTERED);
        return appointmentRepository.save(appointment);
    }

    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Appointment not found: " + id));
    }

    public List<Appointment> findByPatientNic(String nic) {
        Patient patient = patientService.findByNic(nic);
        return appointmentRepository.findAllByPatientId(patient.getId());
    }
}
