package lk.opdqueue.observer;

import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.QueueTicket;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class DisplayBoardNotifier implements QueueEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public DisplayBoardNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onTicketCalled(QueueTicket ticket) {
        DisplayBoardResponse response = new DisplayBoardResponse();
        response.setDepartmentName(ticket.getDepartment().getName());
        response.setCurrentlyCalledTicket(ticket.getTicketNumber());
        messagingTemplate.convertAndSend(
                "/topic/queue/" + ticket.getDepartment().getId(),
                response
        );
    }

    @Override
    public void onQueueUpdated(Department department) {
        messagingTemplate.convertAndSend(
                "/topic/queue/" + department.getId() + "/count",
                department.getCurrentQueueCount()
        );
    }
}