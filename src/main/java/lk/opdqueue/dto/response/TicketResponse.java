package lk.opdqueue.dto.response;

import lk.opdqueue.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketResponse {

    private UUID id;
    private String ticketNumber;
    private String patientName;
    private String departmentName;
    private TicketStatus status;
    private boolean isEmergency;
    private int queuePosition;
    private int estimatedWaitMinutes;
    private String slipDownloadUrl;
    private LocalDateTime issuedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public boolean isEmergency() { return isEmergency; }
    public void setEmergency(boolean emergency) { isEmergency = emergency; }

    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }

    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }

    public String getSlipDownloadUrl() { return slipDownloadUrl; }
    public void setSlipDownloadUrl(String slipDownloadUrl) { this.slipDownloadUrl = slipDownloadUrl; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}