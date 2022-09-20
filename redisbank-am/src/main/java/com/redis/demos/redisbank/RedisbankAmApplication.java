package com.redis.demos.redisbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.redis.spring.lettucemod.RedisModulesAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = RedisModulesAutoConfiguration.class)
public class RedisbankAmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisbankAmApplication.class, args);
	}

}
