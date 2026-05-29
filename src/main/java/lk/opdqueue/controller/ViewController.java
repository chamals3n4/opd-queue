package lk.opdqueue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ViewController {

    @GetMapping("/reception")
    public String reception() {
        return "reception";
    }

    @GetMapping("/status/{ticketNumber}")
    public String patientStatus(@PathVariable String ticketNumber, Model model) {
        model.addAttribute("ticketNumber", ticketNumber);
        return "patient-status";
    }

    @GetMapping("/display/{departmentId}")
    public String displayBoard(@PathVariable Long departmentId, Model model) {
        model.addAttribute("departmentId", departmentId);
        return "display-board";
    }
}
