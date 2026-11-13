package com.opslog.mcp;

import com.opslog.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-coach")
public class AiCoachController {

    private final AiCoachService coach;
    private final CurrentUser currentUser;

    public AiCoachController(AiCoachService coach, CurrentUser currentUser) {
        this.coach = coach;
        this.currentUser = currentUser;
    }

    @GetMapping public AiCoachResponse status() { return coach.status(currentUser.id()); }
}
