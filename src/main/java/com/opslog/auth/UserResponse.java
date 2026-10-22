package com.opslog.auth;

public record UserResponse(Long id, String email, String displayName) {

    static UserResponse from(UserAccount account) {
        return new UserResponse(account.getId(), account.getEmail(), account.getDisplayName());
    }
}
