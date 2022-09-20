package com.redis.demos.redisbankpfm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.redis.spring.lettucemod.RedisModulesAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = RedisModulesAutoConfiguration.class)
public class RedisbankPfmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisbankPfmApplication.class, args);
	}

}
