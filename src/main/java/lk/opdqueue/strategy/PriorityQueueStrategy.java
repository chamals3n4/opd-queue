package lk.opdqueue.strategy;

import lk.opdqueue.entity.QueueTicket;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class PriorityQueueStrategy implements QueueStrategy {

    @Override
    public List<QueueTicket> order(List<QueueTicket> tickets) {
        return tickets.stream()
                .sorted(Comparator
                        .comparing(QueueTicket::isEmergency).reversed()
                        .thenComparing(QueueTicket::getQueuePosition))
                .toList();
    }
}
