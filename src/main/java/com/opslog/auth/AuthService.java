package com.opslog.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
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
