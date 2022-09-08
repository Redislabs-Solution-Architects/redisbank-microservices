package com.redis.demos.redisbankui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RedisbankUiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisbankUiApplication.class, args);
	}

	@Bean
	RestTemplate restTemplate()	{
		return new RestTemplate();
	}

}
