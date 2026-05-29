package lk.opdqueue.dto.response;

import java.util.List;

public class DisplayBoardResponse {

    private String departmentName;
    private String currentlyCalledTicket;
    private List<String> nextTickets;
    private int totalWaiting;

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getCurrentlyCalledTicket() { return currentlyCalledTicket; }
    public void setCurrentlyCalledTicket(String currentlyCalledTicket) { this.currentlyCalledTicket = currentlyCalledTicket; }

    public List<String> getNextTickets() { return nextTickets; }
    public void setNextTickets(List<String> nextTickets) { this.nextTickets = nextTickets; }

    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
}