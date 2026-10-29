package com.opslog.report;

import com.opslog.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/training")
    public TrainingReport training(ReportSearch search) {
        return reportService.training(currentUser.id(), search);
    }

    @GetMapping("/balance")
    public BalanceReport balance(ReportSearch search) {
        return reportService.balance(currentUser.id(), search);
    }
}
