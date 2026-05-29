package lk.opdqueue.service;

import lk.opdqueue.dto.request.IssueTicketRequest;
import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.dto.response.QueueStatusResponse;
import lk.opdqueue.entity.Department;
import lk.opdqueue.entity.Patient;
import lk.opdqueue.entity.QueueTicket;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.exception.DepartmentNotFoundException;
import lk.opdqueue.exception.InvalidTicketStateException;
import lk.opdqueue.exception.QueueFullException;
import lk.opdqueue.exception.TicketNotFoundException;
import lk.opdqueue.observer.QueueEventListener;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.QueueTicketRepository;
import lk.opdqueue.strategy.PriorityQueueStrategy;
import lk.opdqueue.util.TicketNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueueService {

    private static final int AVG_CONSULTATION_MINUTES = 10;

    private final QueueTicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientService patientService;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final PriorityQueueStrategy queueStrategy;
    private final SlipGeneratorService slipGeneratorService;
    private final List<QueueEventListener> eventListeners;

    public QueueService(QueueTicketRepository ticketRepository,
                        DepartmentRepository departmentRepository,
                        PatientService patientService,
                        TicketNumberGenerator ticketNumberGenerator,
                        PriorityQueueStrategy queueStrategy,
                        SlipGeneratorService slipGeneratorService,
                        List<QueueEventListener> eventListeners) {
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
        this.patientService = patientService;
        this.ticketNumberGenerator = ticketNumberGenerator;
        this.queueStrategy = queueStrategy;
        this.slipGeneratorService = slipGeneratorService;
        this.eventListeners = eventListeners;
    }

    @Transactional
    public QueueTicket issueTicket(IssueTicketRequest request) throws Exception {
        Patient patient = patientService.findByNic(request.getNic());

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new DepartmentNotFoundException(
                        "Department not found: " + request.getDepartmentId()));

        if (department.getCurrentQueueCount() >= department.getMaxQueueCapacity()) {
            throw new QueueFullException("Queue is full for department: " + department.getName());
        }

        List<TicketStatus> activeStatuses = List.of(TicketStatus.WAITING, TicketStatus.REGISTERED, TicketStatus.CALLED);
        int maxPosition = ticketRepository
                .findMaxQueuePositionByDepartmentId(department.getId(), activeStatuses)
                .orElse(0);

        QueueTicket ticket = new QueueTicket();
        ticket.setPatient(patient);
        ticket.setDepartment(department);
        ticket.setEmergency(request.isEmergency());
        ticket.setStatus(TicketStatus.WAITING);
        ticket.setQueuePosition(request.isEmergency() ? 1 : maxPosition + 1);
        ticket.setEstimatedWaitMinutes(ticket.getQueuePosition() * AVG_CONSULTATION_MINUTES);
        ticket.setTicketNumber(ticketNumberGenerator.generate(
                department.getDepartmentType().name().substring(0, 3)
        ));

        QueueTicket saved = ticketRepository.save(ticket);

        // generate PDF slip and upload to R2
        String slipUrl = slipGeneratorService.generateAndUpload(saved);
        saved.setSlipR2Key(slipUrl);
        ticketRepository.save(saved);

        department.setCurrentQueueCount(department.getCurrentQueueCount() + 1);
        departmentRepository.save(department);

        eventListeners.forEach(l -> l.onQueueUpdated(department));

        return saved;
    }

    @Transactional
    public QueueTicket callNext(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + departmentId));

        List<QueueTicket> waiting = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.WAITING);

        List<QueueTicket> ordered = queueStrategy.order(waiting);

        if (ordered.isEmpty()) {
            throw new InvalidTicketStateException("No patients waiting in queue for: " + department.getName());
        }

        QueueTicket next = ordered.get(0);
        next.setStatus(TicketStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());
        QueueTicket saved = ticketRepository.save(next);

        eventListeners.forEach(l -> l.onTicketCalled(saved));
        eventListeners.forEach(l -> l.onQueueUpdated(department));

        return saved;
    }

    @Transactional
    public QueueTicket complete(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);
        validateTransition(ticket.getStatus(), TicketStatus.COMPLETED);
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());
        decrementQueue(ticket.getDepartment());
        return ticketRepository.save(ticket);
    }

    @Transactional
    public QueueTicket markNoShow(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);
        validateTransition(ticket.getStatus(), TicketStatus.NO_SHOW);
        ticket.setStatus(TicketStatus.NO_SHOW);
        decrementQueue(ticket.getDepartment());
        return ticketRepository.save(ticket);
    }

    public QueueStatusResponse getStatus(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);

        List<QueueTicket> ahead = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        ticket.getDepartment().getId(), TicketStatus.WAITING)
                .stream()
                .filter(t -> t.getQueuePosition() < ticket.getQueuePosition())
                .toList();

        QueueStatusResponse response = new QueueStatusResponse();
        response.setTicketNumber(ticket.getTicketNumber());
        response.setStatus(ticket.getStatus());
        response.setQueuePosition(ticket.getQueuePosition());
        response.setEstimatedWaitMinutes(ahead.size() * AVG_CONSULTATION_MINUTES);
        response.setPeopleAhead(ahead.size());
        response.setDepartmentName(ticket.getDepartment().getName());
        return response;
    }

    public DisplayBoardResponse getDisplayBoard(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found: " + departmentId));

        List<QueueTicket> waiting = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.WAITING);

        List<QueueTicket> called = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.CALLED);

        List<QueueTicket> ordered = queueStrategy.order(waiting);

        DisplayBoardResponse response = new DisplayBoardResponse();
        response.setDepartmentName(department.getName());
        response.setCurrentlyCalledTicket(called.isEmpty() ? "-" : called.get(0).getTicketNumber());
        response.setNextTickets(ordered.stream().limit(5).map(QueueTicket::getTicketNumber).toList());
        response.setTotalWaiting(waiting.size());
        return response;
    }

    public List<QueueTicket> getDepartmentQueue(Long departmentId) {
        return ticketRepository.findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                departmentId, TicketStatus.WAITING);
    }

    private QueueTicket findByTicketNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketNumber));
    }

    private void validateTransition(TicketStatus current, TicketStatus next) {
        boolean valid = switch (next) {
            case COMPLETED -> current == TicketStatus.IN_PROGRESS || current == TicketStatus.CALLED;
            case NO_SHOW -> current == TicketStatus.CALLED || current == TicketStatus.WAITING;
            case IN_PROGRESS -> current == TicketStatus.CALLED;
            case CANCELLED -> current == TicketStatus.WAITING || current == TicketStatus.REGISTERED;
            default -> false;
        };
        if (!valid) {
            throw new InvalidTicketStateException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private void decrementQueue(Department department) {
        int count = Math.max(0, department.getCurrentQueueCount() - 1);
        department.setCurrentQueueCount(count);
        departmentRepository.save(department);
    }
}