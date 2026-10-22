package com.opslog.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // returns 201 without needing ResponseEntity
    public UserResponse register(@Valid @RequestBody RegisterRequest request) { //runs bean validation annotations on the record before the method body runs.
        return authService.register(request);
    }
}
