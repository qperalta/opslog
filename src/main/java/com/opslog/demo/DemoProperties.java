package com.opslog.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/* OPSLOG_DEMO_ENABLED, OPSLOG_DEMO_RESET, OPSLOG_DEMO_EMAIL, OPSLOG_DEMO_PASSWORD; the password only lives in the environment */
@ConfigurationProperties("opslog.demo")
public record DemoProperties(boolean enabled, boolean reset, String email, String password, String displayName) {}
