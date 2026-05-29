package lk.opdqueue.util;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TicketNumberGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);
    private String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    public synchronized String generate(String departmentPrefix) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(currentDate)) {
            currentDate = today;
            counter.set(0);
        }
        int number = counter.incrementAndGet();
        return String.format("%s-%s-%04d", departmentPrefix, today, number);
    }
}