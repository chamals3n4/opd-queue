package lk.opdqueue.controller;

import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.Staff;
import lk.opdqueue.enums.StaffRole;
import lk.opdqueue.exception.DepartmentNotFoundException;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.StaffRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffController(StaffRepository staffRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAll() {
        return ResponseEntity.ok(staffRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Staff> create(@RequestBody Map<String, Object> body) {
        Staff staff = new Staff();
        staff.setFullName((String) body.get("fullName"));
        staff.setRole(StaffRole.valueOf((String) body.get("role")));
        staff.setUsername((String) body.get("username"));
        String rawPassword = (String) body.get("password");
        if (rawPassword != null && !rawPassword.isBlank()) {
            staff.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        Long deptId = body.get("departmentId") != null
                ? ((Number) body.get("departmentId")).longValue() : null;
        Department dept = deptId != null
                ? departmentRepository.findById(deptId)
                    .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + deptId))
                : departmentRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new DepartmentNotFoundException("No departments exist"));
        staff.setDepartment(dept);
        return ResponseEntity.ok(staffRepository.save(staff));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Staff> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found: " + id));
        if (body.containsKey("fullName")) staff.setFullName((String) body.get("fullName"));
        if (body.containsKey("role")) staff.setRole(StaffRole.valueOf((String) body.get("role")));
        if (body.containsKey("username")) staff.setUsername((String) body.get("username"));
        if (body.containsKey("password") && body.get("password") != null) {
            String pw = (String) body.get("password");
            if (!pw.isBlank()) staff.setPasswordHash(passwordEncoder.encode(pw));
        }
        if (body.containsKey("departmentId") && body.get("departmentId") != null) {
            Long deptId = ((Number) body.get("departmentId")).longValue();
            Department dept = departmentRepository.findById(deptId)
                    .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + deptId));
            staff.setDepartment(dept);
        }
        return ResponseEntity.ok(staffRepository.save(staff));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
