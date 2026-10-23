package com.opslog.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/* user queries goes through here */
@Component
public class CurrentUser {

    public long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof JwtAuthenticationToken jwtAuth)
            return Long.valueOf(jwtAuth.getToken().getSubject());
        throw new IllegalStateException("No authenticated user in context");
    }
}
