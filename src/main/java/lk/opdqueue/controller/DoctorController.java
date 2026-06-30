package lk.opdqueue.controller;

import lk.opdqueue.exception.AppException;
import lk.opdqueue.model.Department;
import lk.opdqueue.model.Doctor;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    public DoctorController(DoctorRepository doctorRepository,
                            DepartmentRepository departmentRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> getAll() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<Doctor>> getByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(doctorRepository.findAllByDepartmentId(deptId));
    }

    @PostMapping
    public ResponseEntity<Doctor> create(@RequestBody Map<String, Object> body) {
        Doctor doc = new Doctor();
        doc.setFullName((String) body.get("fullName"));
        doc.setSpecialization((String) body.get("specialization"));
        doc.setRoomNumber((String) body.get("roomNumber"));

        Long deptId = body.get("departmentId") != null
                ? ((Number) body.get("departmentId")).longValue() : null;
        Department dept = deptId != null
                ? departmentRepository.findById(deptId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Department not found: " + deptId))
                : departmentRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"No departments exist"));
        doc.setDepartment(dept);
        return ResponseEntity.ok(doctorRepository.save(doc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Doctor doc = doctorRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Doctor not found: " + id));
        if (body.containsKey("fullName")) doc.setFullName((String) body.get("fullName"));
        if (body.containsKey("specialization")) doc.setSpecialization((String) body.get("specialization"));
        if (body.containsKey("roomNumber")) doc.setRoomNumber((String) body.get("roomNumber"));
        if (body.containsKey("isAvailable") && body.get("isAvailable") != null) {
            doc.setAvailable((Boolean) body.get("isAvailable"));
        }
        if (body.containsKey("departmentId") && body.get("departmentId") != null) {
            Long deptId = ((Number) body.get("departmentId")).longValue();
            Department dept = departmentRepository.findById(deptId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"Department not found: " + deptId));
            doc.setDepartment(dept);
        }
        return ResponseEntity.ok(doctorRepository.save(doc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
