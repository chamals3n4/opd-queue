package lk.opdqueue.observer;

import lk.opdqueue.dto.response.QueueStatusResponse;
import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.repository.QueueTicketRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientNotifier implements QueueEventListener {

    private static final int AVG_CONSULTATION_MINUTES = 10;

    private final SimpMessagingTemplate messagingTemplate;
    private final QueueTicketRepository ticketRepository;

    public PatientNotifier(SimpMessagingTemplate messagingTemplate,
                           QueueTicketRepository ticketRepository) {
        this.messagingTemplate = messagingTemplate;
        this.ticketRepository = ticketRepository;
    }

    private QueueStatusResponse buildStatus(QueueTicket ticket) {
        List<QueueTicket> ahead = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        ticket.getDepartment().getId(), TicketStatus.WAITING)
                .stream()
                .filter(t -> t.getQueuePosition() < ticket.getQueuePosition())
                .toList();

        QueueStatusResponse resp = new QueueStatusResponse();
        resp.setTicketNumber(ticket.getTicketNumber());
        resp.setStatus(ticket.getStatus());
        resp.setQueuePosition(ticket.getQueuePosition());
        resp.setEstimatedWaitMinutes(ahead.size() * AVG_CONSULTATION_MINUTES);
        resp.setPeopleAhead(ahead.size());
        resp.setDepartmentName(ticket.getDepartment().getName());
        return resp;
    }

    @Override
    public void onTicketCalled(QueueTicket ticket) {
        messagingTemplate.convertAndSend("/topic/ticket/" + ticket.getTicketNumber(),
                buildStatus(ticket));
    }

    @Override
    public void onTicketIssued(QueueTicket ticket) {
        messagingTemplate.convertAndSend("/topic/ticket/" + ticket.getTicketNumber(),
                buildStatus(ticket));
    }

    @Override
    public void onQueueUpdated(Department department) {
        // department-wide updates are handled by DisplayBoardNotifier
    }
}
