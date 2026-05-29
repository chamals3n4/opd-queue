package lk.opdqueue.observer;

import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.QueueTicket;

public interface QueueEventListener {
    void onTicketCalled(QueueTicket ticket);
    void onQueueUpdated(Department department);
}