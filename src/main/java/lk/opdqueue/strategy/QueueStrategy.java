package lk.opdqueue.strategy;

import lk.opdqueue.entity.QueueTicket;
import java.util.List;

public interface QueueStrategy {
    List<QueueTicket> order(List<QueueTicket> tickets);
}