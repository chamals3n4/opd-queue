package lk.opdqueue.observer;

import lk.opdqueue.dto.response.QueueStatusResponse;
import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.QueueTicket;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class PatientNotifier implements QueueEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public PatientNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onTicketCalled(QueueTicket ticket) {
        QueueStatusResponse response = new QueueStatusResponse();
        response.setTicketNumber(ticket.getTicketNumber());
        response.setStatus(ticket.getStatus());
        response.setQueuePosition(ticket.getQueuePosition());
        response.setEstimatedWaitMinutes(ticket.getEstimatedWaitMinutes());
        response.setDepartmentName(ticket.getDepartment().getName());
        messagingTemplate.convertAndSend(
                "/topic/ticket/" + ticket.getTicketNumber(),
                response
        );
    }

    @Override
    public void onQueueUpdated(Department department) {
        // patient level update not needed for department-wide events
    }
}