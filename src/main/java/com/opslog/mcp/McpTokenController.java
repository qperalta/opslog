package com.opslog.mcp;

import com.opslog.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/mcp-tokens")
public class McpTokenController {

    private final McpTokenService tokenService;
    private final CurrentUser currentUser;

    public McpTokenController(McpTokenService tokenService, CurrentUser currentUser) {
        this.tokenService = tokenService;
        this.currentUser = currentUser;
    }

    @GetMapping public List<McpTokenResponse> list() { return tokenService.list(currentUser.id()); }

    @PostMapping public ResponseEntity<IssuedMcpTokenResponse> issue(@Valid @RequestBody McpTokenRequest request) {
        IssuedMcpTokenResponse issued = tokenService.issue(currentUser.id(), request);
        return ResponseEntity.created(URI.create("/api/mcp-tokens/" + issued.token().id())).body(issued);
    }

    @PostMapping("/{id}/revoke")
    public McpTokenResponse revoke(@PathVariable long id) {
        return tokenService.revoke(currentUser.id(), id);
    }
}
