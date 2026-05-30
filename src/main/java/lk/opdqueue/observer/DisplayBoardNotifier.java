package lk.opdqueue.observer;

import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.QueueTicketRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisplayBoardNotifier implements QueueEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final QueueTicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;

    public DisplayBoardNotifier(SimpMessagingTemplate messagingTemplate,
                                QueueTicketRepository ticketRepository,
                                DepartmentRepository departmentRepository) {
        this.messagingTemplate = messagingTemplate;
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
    }

    private DisplayBoardResponse buildBoard(Long deptId) {
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept == null) return null;

        List<QueueTicket> waiting = ticketRepository.findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                deptId, TicketStatus.WAITING);
        List<QueueTicket> called = ticketRepository.findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                deptId, TicketStatus.CALLED);

        DisplayBoardResponse resp = new DisplayBoardResponse();
        resp.setDepartmentName(dept.getName());
        resp.setCurrentlyCalledTicket(called.isEmpty() ? "-" : called.get(0).getTicketNumber());
        resp.setNextTickets(waiting.stream().limit(5).map(QueueTicket::getTicketNumber).toList());
        resp.setTotalWaiting(waiting.size());
        return resp;
    }

    private void pushBoard(Long deptId) {
        DisplayBoardResponse board = buildBoard(deptId);
        if (board != null) {
            messagingTemplate.convertAndSend("/topic/queue/" + deptId, board);
        }
    }

    @Override
    public void onTicketCalled(QueueTicket ticket) {
        pushBoard(ticket.getDepartment().getId());
    }

    @Override
    public void onTicketIssued(QueueTicket ticket) {
        pushBoard(ticket.getDepartment().getId());
    }

    @Override
    public void onQueueUpdated(Department department) {
        pushBoard(department.getId());
    }
}
