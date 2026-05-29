package lk.opdqueue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class IssueTicketRequest {

    @NotBlank(message = "NIC is required")
    private String nic;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private boolean emergency;

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }
}