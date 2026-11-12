package com.opslog.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/* reads "Authorization: Bearer opslog_..." on /mcp; no match leaves the request anonymous, so it gets a 401
 *  not a @Component: Spring Boot would also register it for every request outside the security chain */
public class McpTokenFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    private final McpTokenService tokens;

    public McpTokenFilter(McpTokenService tokens) { this.tokens = tokens; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(header != null && header.startsWith(BEARER)) {
            tokens.authenticate(header.substring(BEARER.length()).trim())
                    .ifPresent(principal -> {
                        var authority = new SimpleGrantedAuthority("MCP_" + principal.scope().name());
                        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
