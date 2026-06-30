package lk.opdqueue.service;

import lk.opdqueue.dto.request.IssueTicketRequest;
import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.dto.response.QueueStatusResponse;
import lk.opdqueue.enums.TicketStatus;
import lk.opdqueue.exception.AppException;
import lk.opdqueue.model.Department;
import lk.opdqueue.model.Patient;
import lk.opdqueue.model.QueueTicket;
import lk.opdqueue.repository.DepartmentRepository;
import lk.opdqueue.repository.QueueTicketRepository;
import lk.opdqueue.util.TicketNumberGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class QueueService {

    private final QueueTicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientService patientService;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final SimpMessagingTemplate messaging;

    // used to open a fresh read-only tx after the main one commits, so websocket gets committed data
    private final TransactionTemplate readOnlyTx;


    public QueueService(QueueTicketRepository ticketRepository,
                        DepartmentRepository departmentRepository,
                        PatientService patientService,
                        TicketNumberGenerator ticketNumberGenerator,
                        SimpMessagingTemplate messaging,
                        PlatformTransactionManager txManager) {
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
        this.patientService = patientService;
        this.ticketNumberGenerator = ticketNumberGenerator;
        this.messaging = messaging;
        this.readOnlyTx = new TransactionTemplate(txManager);
        this.readOnlyTx.setReadOnly(true);
    }


    @Transactional
    public QueueTicket issueTicket(IssueTicketRequest request) throws Exception {
        Patient patient = patientService.findByNic(request.getNic());

        // don't let the same patient join twice
        List<TicketStatus> activeStatuses = List.of(TicketStatus.WAITING, TicketStatus.CALLED, TicketStatus.IN_PROGRESS);
        if (ticketRepository.existsByPatientIdAndStatusIn(patient.getId(), activeStatuses)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Patient " + patient.getFullName() + " already has an active ticket in the queue.");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Department not found: " + request.getDepartmentId()));

        if (department.getCurrentQueueCount() >= department.getMaxQueueCapacity()) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Queue is full for department: " + department.getName());
        }

        // find the current highest position so we can slot the new ticket after it
        List<TicketStatus> positionStatuses = List.of(TicketStatus.WAITING, TicketStatus.REGISTERED, TicketStatus.CALLED);
        int maxPosition = ticketRepository
                .findMaxQueuePositionByDepartmentId(department.getId(), positionStatuses)
                .orElse(0);

        QueueTicket ticket = new QueueTicket();
        ticket.setPatient(patient);
        ticket.setDepartment(department);
        ticket.setEmergency(request.isEmergency());
        ticket.setStatus(TicketStatus.WAITING);
        ticket.setQueuePosition(request.isEmergency() ? 1 : maxPosition + 1); // emergency always goes to position 1
        ticket.setTicketNumber(ticketNumberGenerator.generate(
                department.getDepartmentType().name().substring(0, 3)));

        QueueTicket saved = ticketRepository.save(ticket);
        department.setCurrentQueueCount(department.getCurrentQueueCount() + 1);
        departmentRepository.save(department);

        pushBoard(department.getId());
        pushTicketStatus(saved);

        return saved;
    }


    @Transactional
    public QueueTicket callNext(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentId));

        List<QueueTicket> waiting = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.WAITING);

        List<QueueTicket> ordered = sortTickets(waiting);
        if (ordered.isEmpty()) {
            throw new AppException(HttpStatus.CONFLICT,
                    "No patients waiting in queue for: " + department.getName());
        }

        QueueTicket next = ordered.get(0);
        next.setStatus(TicketStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());
        QueueTicket saved = ticketRepository.save(next);

        pushBoard(departmentId);
        pushTicketStatus(saved);

        return saved;
    }


    @Transactional
    public QueueTicket complete(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);
        validateTransition(ticket.getStatus(), TicketStatus.COMPLETED);
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());
        decrementQueue(ticket.getDepartment());
        QueueTicket saved = ticketRepository.save(ticket);

        pushBoard(ticket.getDepartment().getId());
        pushTicketStatus(saved);

        return saved;
    }


    @Transactional
    public QueueTicket markNoShow(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);
        validateTransition(ticket.getStatus(), TicketStatus.NO_SHOW);
        ticket.setStatus(TicketStatus.NO_SHOW);
        decrementQueue(ticket.getDepartment());
        QueueTicket saved = ticketRepository.save(ticket);

        pushBoard(ticket.getDepartment().getId());

        return saved;
    }


    @Transactional
    public void resetDepartmentQueue(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentId));

        // cancel all waiting tickets and reset the count
        List<QueueTicket> active = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.WAITING);
        active.forEach(t -> t.setStatus(TicketStatus.CANCELLED));
        ticketRepository.saveAll(active);

        department.setCurrentQueueCount(0);
        departmentRepository.save(department);
        pushBoard(departmentId);
    }


    public QueueStatusResponse getStatus(String ticketNumber) {
        QueueTicket ticket = findByTicketNumber(ticketNumber);

        QueueStatusResponse response = new QueueStatusResponse();
        response.setTicketNumber(ticket.getTicketNumber());
        response.setStatus(ticket.getStatus());
        response.setQueuePosition(ticket.getQueuePosition());
        response.setDepartmentName(ticket.getDepartment().getName());
        return response;
    }


    public DisplayBoardResponse getDisplayBoard(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentId));

        List<QueueTicket> waiting = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.WAITING);
        List<QueueTicket> called = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                        departmentId, TicketStatus.CALLED);

        List<QueueTicket> sortedWaiting = sortTickets(waiting);
        QueueTicket calledTicket = called.isEmpty() ? null : called.get(0);

        DisplayBoardResponse response = new DisplayBoardResponse();
        response.setDepartmentName(department.getName());
        response.setCurrentlyCalledTicket(calledTicket != null ? calledTicket.getTicketNumber() : "-");
        response.setCurrentEmergency(calledTicket != null && calledTicket.isEmergency());
        response.setNextTickets(sortedWaiting.stream().limit(5).map(QueueTicket::getTicketNumber).toList());
        response.setEmergencyNextTickets(sortedWaiting.stream().limit(5).filter(QueueTicket::isEmergency).map(QueueTicket::getTicketNumber).toList());
        response.setTotalWaiting(waiting.size());
        return response;
    }


    public List<QueueTicket> getDepartmentQueue(Long departmentId) {
        return ticketRepository.findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(
                departmentId, TicketStatus.WAITING);
    }


    // emergencies first, then by position number
    private List<QueueTicket> sortTickets(List<QueueTicket> tickets) {
        return tickets.stream()
                .sorted(Comparator.comparing(QueueTicket::isEmergency).reversed()
                        .thenComparing(QueueTicket::getQueuePosition))
                .toList();
    }


    // waits for the current transaction to commit before pushing, so the board reads fresh data
    private void pushBoard(Long deptId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    readOnlyTx.execute(status -> {
                        messaging.convertAndSend("/topic/queue/" + deptId, buildBoard(deptId));
                        return null;
                    });
                }
            });
        } else {
            messaging.convertAndSend("/topic/queue/" + deptId, buildBoard(deptId));
        }
    }


    // sends this ticket's current status to the patient's own status page
    private void pushTicketStatus(QueueTicket ticket) {
        QueueStatusResponse status = new QueueStatusResponse();
        status.setTicketNumber(ticket.getTicketNumber());
        status.setStatus(ticket.getStatus());
        status.setQueuePosition(ticket.getQueuePosition());
        status.setDepartmentName(ticket.getDepartment().getName());
        messaging.convertAndSend("/topic/ticket/" + ticket.getTicketNumber(), status);
    }


    private DisplayBoardResponse buildBoard(Long deptId) {
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept == null) return new DisplayBoardResponse();

        List<QueueTicket> waiting = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(deptId, TicketStatus.WAITING);
        List<QueueTicket> called = ticketRepository
                .findAllByDepartmentIdAndStatusOrderByIsEmergencyDescQueuePositionAsc(deptId, TicketStatus.CALLED);

        List<QueueTicket> sortedWaiting = sortTickets(waiting);
        QueueTicket calledTicket = called.isEmpty() ? null : called.get(0);

        DisplayBoardResponse board = new DisplayBoardResponse();
        board.setDepartmentName(dept.getName());
        board.setCurrentlyCalledTicket(calledTicket != null ? calledTicket.getTicketNumber() : "-");
        board.setCurrentEmergency(calledTicket != null && calledTicket.isEmergency());
        board.setNextTickets(sortedWaiting.stream().limit(5).map(QueueTicket::getTicketNumber).toList());
        board.setEmergencyNextTickets(sortedWaiting.stream().limit(5).filter(QueueTicket::isEmergency).map(QueueTicket::getTicketNumber).toList());
        board.setTotalWaiting(waiting.size());
        return board;
    }


    private QueueTicket findByTicketNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Ticket not found: " + ticketNumber));
    }


    // makes sure the status change is a valid step in the lifecycle
    private void validateTransition(TicketStatus current, TicketStatus next) {
        boolean valid = switch (next) {
            case COMPLETED   -> current == TicketStatus.IN_PROGRESS || current == TicketStatus.CALLED;
            case NO_SHOW     -> current == TicketStatus.CALLED       || current == TicketStatus.WAITING;
            case IN_PROGRESS -> current == TicketStatus.CALLED;
            case CANCELLED   -> current == TicketStatus.WAITING      || current == TicketStatus.REGISTERED;
            default -> false;
        };
        if (!valid) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Cannot transition from " + current + " to " + next);
        }
    }


    // won't go below 0 just in case something gets out of sync
    private void decrementQueue(Department department) {
        department.setCurrentQueueCount(Math.max(0, department.getCurrentQueueCount() - 1));
        departmentRepository.save(department);
    }

}
