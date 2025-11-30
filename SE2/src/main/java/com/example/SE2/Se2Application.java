package com.example.SE2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Se2Application {

	public static void main(String[] args) {
		SpringApplication.run(Se2Application.class, args);
	}

}
