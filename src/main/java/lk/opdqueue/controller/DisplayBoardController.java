package lk.opdqueue.controller;

import lk.opdqueue.dto.response.DisplayBoardResponse;
import lk.opdqueue.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/display")
public class DisplayBoardController {

    private final QueueService queueService;

    public DisplayBoardController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DisplayBoardResponse> getBoard(@PathVariable Long departmentId) {
        return ResponseEntity.ok(queueService.getDisplayBoard(departmentId));
    }
}