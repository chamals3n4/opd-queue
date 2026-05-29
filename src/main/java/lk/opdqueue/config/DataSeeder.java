package lk.opdqueue.config;

import lk.opdqueue.entity.Department;
import lk.opdqueue.enums.DepartmentType;
import lk.opdqueue.repository.DepartmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;

    public DataSeeder(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() > 0) return;

        departmentRepository.save(createDept("General OPD", DepartmentType.OPD_GENERAL, 50));
        departmentRepository.save(createDept("Cardiology", DepartmentType.CARDIOLOGY, 30));
        departmentRepository.save(createDept("Pediatrics", DepartmentType.PEDIATRICS, 30));
        departmentRepository.save(createDept("Orthopedics", DepartmentType.ORTHOPEDICS, 25));
        departmentRepository.save(createDept("Neurology", DepartmentType.NEUROLOGY, 20));
        departmentRepository.save(createDept("Gynecology", DepartmentType.GYNECOLOGY, 25));
    }

    private Department createDept(String name, DepartmentType type, int capacity) {
        Department d = new Department();
        d.setName(name);
        d.setDepartmentType(type);
        d.setMaxQueueCapacity(capacity);
        return d;
    }
}
