package com.opslog.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String email, String passwordHash, String displayName) {
        setEmail(email);
        setPasswordHash(passwordHash);
        setDisplayName(displayName);
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEmail(String email) {
        if(email == null || email.isBlank()) throw new  IllegalArgumentException("Email cannot be null or empty");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if(normalizedEmail.length() > 254 || !normalizedEmail.contains("@")) throw new  IllegalArgumentException("Email address is invalid");
        this.email = normalizedEmail;
    }

    public void setPasswordHash(String passwordHash) {
        if(passwordHash == null  || passwordHash.isBlank()) throw new  IllegalArgumentException("Password hash cannot be null or empty");
        this.passwordHash = passwordHash;
    }

    public void setDisplayName(String displayName) {
        if(displayName == null || displayName.isBlank()) throw new  IllegalArgumentException("Display name cannot be null or empty");
        String trimmedName = displayName.trim();
        if(trimmedName.length() > 50) throw new  IllegalArgumentException("Display name is invalid");
        this.displayName = trimmedName;
    }
}
