package com.opslog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpslogApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpslogApplication.class, args);
	}

}
