package com.demos.redis.redisbankdatageneration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedisbankDatagenerationApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisbankDatagenerationApplication.class, args);
	}

}
