package com.pxa.wex.transactiondb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class WexApplication {

	public static void main(String[] args) {
		SpringApplication.run(WexApplication.class, args);
	}

}
