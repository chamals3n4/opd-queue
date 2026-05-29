package lk.opdqueue.repository;

import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueTicketRepository extends JpaRepository<QueueTicket, UUID> {
    Optional<QueueTicket> findByTicketNumber(String ticketNumber);
    boolean existsByTicketNumber(String ticketNumber);

    List<QueueTicket> findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
            Long departmentId, TicketStatus status
    );

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.department.id = :deptId AND q.status = :status")
    int countByDepartmentIdAndStatus(@Param("deptId") Long deptId, @Param("status") TicketStatus status);

    @Query("SELECT MAX(q.queuePosition) FROM QueueTicket q WHERE q.department.id = :deptId AND q.status IN :statuses")
    Optional<Integer> findMaxQueuePositionByDepartmentId(
            @Param("deptId") Long deptId,
            @Param("statuses") List<TicketStatus> statuses
    );
}