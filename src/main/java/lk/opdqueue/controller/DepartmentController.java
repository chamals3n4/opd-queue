package lk.opdqueue.controller;

import lk.opdqueue.entity.Department;
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
    public ResponseEntity<List<Department>> listActive() {
        return ResponseEntity.ok(departmentRepository.findAllByIsActiveTrue());
    }
}
