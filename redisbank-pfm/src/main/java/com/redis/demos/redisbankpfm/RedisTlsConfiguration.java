package com.redis.demos.redisbankpfm;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration()
@Profile("tls")
public class RedisTlsConfiguration {

	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory(@Value("${spring.redis.host}") String host,
			@Value("${spring.redis.port}") int port) {

		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.useSsl().disablePeerVerification()
				.and()
				.commandTimeout(Duration.ofSeconds(5))
				.shutdownTimeout(Duration.ZERO)
				.build();

		return new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(host, port), clientConfig);
	}
    
}
