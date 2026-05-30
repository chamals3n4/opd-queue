package lk.opdqueue.util;

import lk.opdqueue.repository.QueueTicketRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicketNumberGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);
    private String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    private final QueueTicketRepository ticketRepository;
    private boolean initialized = false;

    public TicketNumberGenerator(QueueTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public synchronized String generate(String departmentPrefix) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (!initialized || !today.equals(currentDate)) {
            currentDate = today;
            String pattern = "%-" + today + "-%";
            int maxFromDb = ticketRepository.findMaxTicketSequenceForDate(today).orElse(0);
            counter.set(maxFromDb);
            initialized = true;
        }

        int number = counter.incrementAndGet();
        return String.format("%s-%s-%04d", departmentPrefix, today, number);
    }
}