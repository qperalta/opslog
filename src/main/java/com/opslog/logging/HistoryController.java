package com.opslog.logging;

import com.opslog.auth.CurrentUser;
import com.opslog.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final CurrentUser currentUser;

    public HistoryController(HistoryService historyService, CurrentUser currentUser) {
        this.historyService = historyService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<HistoryRow> search(@Valid HistorySearch search) {
        return historyService.search(currentUser.id(), search);
    }
}
