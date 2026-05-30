package lk.opdqueue.controller;

import lk.opdqueue.entity.Department;
import lk.opdqueue.exception.DepartmentNotFoundException;
import lk.opdqueue.repository.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentRepository.findAllByIsActiveTrue());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Department>> getAllAdmin() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Department> create(@RequestBody Department department) {
        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable Long id, @RequestBody Department update) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + id));
        dept.setName(update.getName());
        dept.setDepartmentType(update.getDepartmentType());
        dept.setMaxQueueCapacity(update.getMaxQueueCapacity());
        dept.setActive(update.isActive());
        return ResponseEntity.ok(departmentRepository.save(dept));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
