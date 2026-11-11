package com.opslog;

import com.opslog.auth.UserAccount;
import com.opslog.auth.UserAccountRepository;
import com.opslog.mcp.McpScope;
import com.opslog.mcp.McpToken;
import com.opslog.mcp.McpTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class McpTokenSchemaTests {

    @Autowired
    UserAccountRepository users;
    @Autowired
    McpTokenRepository tokens;

    @Test
    void tokenIsFoundByHashAndScopedToItsOwner() {
        UserAccount owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        UserAccount other = users.save(new UserAccount("other@example.com", "x".repeat(60), "Other"));
        McpToken saved = tokens.saveAndFlush(new McpToken(owner.getId(), "Claude", "b".repeat(64), McpScope.READ));

        assertEquals(saved.getId(), tokens.findByTokenHash("b".repeat(64)).orElseThrow().getId());
        assertTrue(tokens.findByIdAndUserId(saved.getId(), owner.getId()).isPresent());
        assertTrue(tokens.findByIdAndUserId(saved.getId(), other.getId()).isEmpty());
        assertEquals(1, tokens.findByUserIdOrderByCreatedAtDesc(owner.getId()).size());
    }

    @Test
    void hashIsUnique() {
        UserAccount owner = users.save(new UserAccount("owner@example.com", "x".repeat(60), "Owner"));
        tokens.saveAndFlush(new McpToken(owner.getId(), "One", "c".repeat(64), McpScope.READ));

        assertThrows(DataIntegrityViolationException.class,
                () -> tokens.saveAndFlush(new McpToken(owner.getId(), "Two", "c".repeat(64), McpScope.READ)));
    }
}
