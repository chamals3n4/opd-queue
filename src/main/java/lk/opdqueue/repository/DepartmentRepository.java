package lk.opdqueue.repository;

import lk.opdqueue.model.Department;
import lk.opdqueue.enums.DepartmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentType(DepartmentType departmentType);
    List<Department> findAllByIsActiveTrue();
}