package com.opslog;

import com.opslog.auth.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginLimitApiTests {

    private static final String EMAIL = "login-limit@example.com";

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;

    @AfterEach
    void tearDown() {
        users.findByEmail(EMAIL).ifPresent(users::delete);
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(EMAIL, password)));
    }

    @Test
    void sixthAttemptAfterFiveFailuresIsRefusedWith429() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"password123","displayName":"Kai"}""".formatted(EMAIL)))
                .andExpect(status().isCreated());

        for(int i = 0; i < 5; i++) login("wrong-password").andExpect(status().isUnauthorized());

        login("password123")
                .andExpect(status().isTooManyRequests())
                // 900 s minus the few milliseconds since the fifth failure, rounded up
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.detail").value("Too many failed sign-ins. Try again in 15 minutes."));
    }
}
