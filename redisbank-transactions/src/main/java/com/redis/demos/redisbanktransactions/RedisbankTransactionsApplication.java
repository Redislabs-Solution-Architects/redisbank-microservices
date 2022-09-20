package com.redis.demos.redisbanktransactions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.redis.spring.lettucemod.RedisModulesAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = RedisModulesAutoConfiguration.class)
public class RedisbankTransactionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisbankTransactionsApplication.class, args);
	}

}
