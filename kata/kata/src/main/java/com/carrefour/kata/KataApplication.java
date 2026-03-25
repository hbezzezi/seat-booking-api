package com.carrefour.kata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KataApplication {

	public static void main(String[] args) {
		SpringApplication.run(KataApplication.class, args);
	}

}
