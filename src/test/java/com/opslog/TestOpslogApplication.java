package com.opslog;

import org.springframework.boot.SpringApplication;

public class TestOpslogApplication {

	public static void main(String[] args) {
		SpringApplication.from(OpslogApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
