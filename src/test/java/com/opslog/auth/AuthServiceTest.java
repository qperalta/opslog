package com.opslog.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock UserAccountRepository users;
    @Mock TokenService tokenService;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    LoginAttempts attempts = new LoginAttempts(new LoginAttemptsTest.TestClock());
    AuthService authService;

    @BeforeEach
    void setup() {
        authService = new AuthService(users, passwordEncoder, tokenService, attempts);
    }

    @Test
    void registerWithExistingEmailThrowsAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest(" Que@email.com ", "password123", "Q");
        when(users.existsByEmail("que@email.com")).thenReturn(true);

        EmailAlreadyRegisteredException ex = assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register(request));
        assertEquals("An account with this email already exists", ex.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    void registerSavesNormalizedEmailAndHashedPassword() {
        RegisterRequest request = new RegisterRequest(" Que@Email.com ", "password123", "Q");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(captor.capture());
        UserAccount saved = captor.getValue();

        assertEquals("que@email.com", saved.getEmail());
        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", saved.getPasswordHash()));
        assertEquals("que@email.com", response.email());
    }

    @Test
    void loginWithWrongPasswordThrowsAndIssuesNoToken() {
        UserAccount account = new UserAccount("que@email.com", passwordEncoder.encode("password123"), "Q");
        when(users.findByEmail("que@email.com")).thenReturn(Optional.of(account));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("que@email.com", "wrong-password")));

        verify(tokenService, never()).issue(any());
    }

    @Test
    void loginWithUnknownEmailThrowsSameExceptionAndIssuesNoToken() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@email.com", "password123")));

        verify(tokenService, never()).issue(any());
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() {
        UserAccount account = new UserAccount("que@email.com", passwordEncoder.encode("password123"), "Q");
        when(users.findByEmail("que@email.com")).thenReturn(Optional.of(account));

        Instant expiresAt = Instant.now().plusSeconds(3600);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("1")
                .expiresAt(expiresAt)
                .build();
        when(tokenService.issue(account)).thenReturn(jwt);

        LoginResponse response = authService.login(new LoginRequest(" Que@Email.com ", "password123"));

        assertEquals("test-token", response.token());
        assertEquals(expiresAt, response.expiresAt());
        assertEquals("que@email.com", response.user().email());
    }

    @Test
    void fiveWrongPasswordsLockTheEmailEvenForTheRightPassword() {
        UserAccount user = new UserAccount("que@email.com", passwordEncoder.encode("password123"), "Q");
        when(users.findByEmail("que@email.com")).thenReturn(Optional.of(user));
        for(int i = 0; i < LoginAttempts.MAX_FAILURES; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(new LoginRequest("que@email.com", "wrong-password")));
        }

        // the lock is checked before the password, and the email is normalized first
        assertThrows(TooManyLoginAttemptsException.class,
                () -> authService.login(new LoginRequest(" QUE@email.com ", "password123")));
        verify(tokenService, never()).issue(any());
    }

    @Test
    void successfulLoginClearsEarlierFailures() {
        UserAccount user = new UserAccount("que@email.com", passwordEncoder.encode("password123"), "Q");
        when(users.findByEmail("que@email.com")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(Jwt.withTokenValue("t").header("alg", "HS256").claim("sub", "1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build());
        for(int i = 0; i < LoginAttempts.MAX_FAILURES - 1; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(new LoginRequest("que@email.com", "wrong-password")));
        }

        authService.login(new LoginRequest("que@email.com", "password123"));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("que@email.com", "wrong-password")));
        assertDoesNotThrow(() -> attempts.checkAllowed("que@email.com"));
    }
}
