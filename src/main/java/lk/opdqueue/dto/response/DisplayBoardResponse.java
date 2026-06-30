package lk.opdqueue.dto.response;

import java.util.List;

public class DisplayBoardResponse {

    private String departmentName;
    private String currentlyCalledTicket;
    private boolean currentEmergency;
    private List<String> nextTickets;
    private List<String> emergencyNextTickets;
    private int totalWaiting;

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getCurrentlyCalledTicket() { return currentlyCalledTicket; }
    public void setCurrentlyCalledTicket(String currentlyCalledTicket) { this.currentlyCalledTicket = currentlyCalledTicket; }

    public boolean isCurrentEmergency() { return currentEmergency; }
    public void setCurrentEmergency(boolean currentEmergency) { this.currentEmergency = currentEmergency; }

    public List<String> getNextTickets() { return nextTickets; }
    public void setNextTickets(List<String> nextTickets) { this.nextTickets = nextTickets; }

    public List<String> getEmergencyNextTickets() { return emergencyNextTickets; }
    public void setEmergencyNextTickets(List<String> emergencyNextTickets) { this.emergencyNextTickets = emergencyNextTickets; }

    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
}