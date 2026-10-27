package com.opslog.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/* React routes; a refresh or deep link on one of these serves index.html and React Router takes over */
@Controller
public class SpaController {

    public static final String[] ROUTES = {"/login", "/register", "/checkin", "/exercises", "/locations"};

    @GetMapping({"/login", "/register", "/checkin", "/exercises", "/locations"})
    public String index() {
        return "forward:/index.html";
    }
}
