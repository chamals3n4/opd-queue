package lk.opdqueue.config;

import lk.opdqueue.enums.DepartmentType;
import lk.opdqueue.enums.StaffRole;
import lk.opdqueue.model.Department;
import lk.opdqueue.model.Staff;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.StaffRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DepartmentRepository departmentRepository,
                      StaffRepository staffRepository,
                      PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            Department dept = new Department();
            dept.setName("OPD");
            dept.setDepartmentType(DepartmentType.OPD);
            dept.setMaxQueueCapacity(100);
            departmentRepository.save(dept);
        }

        if (staffRepository.count() == 0) {
            Department dept = departmentRepository.findAll().get(0);
            Staff admin = new Staff();
            admin.setFullName("System Admin");
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(StaffRole.ADMIN);
            admin.setDepartment(dept);
            staffRepository.save(admin);
        }
    }
}
