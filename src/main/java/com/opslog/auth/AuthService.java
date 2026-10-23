package com.opslog.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyHash;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = UserAccount.normalizeEmail(request.email());
        Optional<UserAccount> user = users.findByEmail(email);

        String hash = user.map(UserAccount::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if(user.isEmpty() || !matches) throw new InvalidCredentialsException();

        Jwt jwt = tokenService.issue(user.get());
        return new LoginResponse(jwt.getTokenValue(), jwt.getExpiresAt(), UserResponse.from(user.get()));
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = UserAccount.normalizeEmail(request.email());
        if(users.existsByEmail(email)) throw new EmailAlreadyRegisteredException();

        String hash = passwordEncoder.encode(request.password());
        UserAccount saved = users.save(new UserAccount(email, hash, request.displayName()));
        return UserResponse.from(saved);
    }
}
