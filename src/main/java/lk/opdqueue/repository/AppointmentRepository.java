package lk.opdqueue.repository;

import lk.opdqueue.model.Appointment;
import lk.opdqueue.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findAllByDoctorIdAndStatus(Long doctorId, TicketStatus status);
    List<Appointment> findAllByPatientId(UUID patientId);
}