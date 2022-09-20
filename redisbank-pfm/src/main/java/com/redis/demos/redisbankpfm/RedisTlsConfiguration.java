package com.redis.demos.redisbankpfm;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/*
 * Don't do this in production folks. TLS and certs are important.
 */
@Configuration()
@Profile("tls")
public class RedisTlsConfiguration {

	/*
	 * This method creates the connection factory to connect to the transactions event bus (Redis, can be A-A)
	 */
	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory(@Value("${spring.redis.host-tr}") String host,
			@Value("${spring.redis.port-tr}") int port) {

		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.useSsl().disablePeerVerification()
				.and()
				.commandTimeout(Duration.ofSeconds(5))
				.shutdownTimeout(Duration.ZERO)
				.build();

		RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration(host, port);

		return new LettuceConnectionFactory(rsc, clientConfig);
	}

	/*
	 * This method creates a client to connect to the local database (Redis, has TimeSeries, so preferably no A-A)
	 */
	@Bean(name = "redisModulesConnection", destroyMethod = "close")
	StatefulRedisModulesConnection<String, String> statefulRedisModulesConnection(
			@Value("${spring.redis.host}") String host,
			@Value("${spring.redis.port}") int port, @Value("${spring.redis.password}") String password) {
		ClientResources clientResources = DefaultClientResources.create();
		RedisURI redisURI = RedisURI.builder().withHost(host).withPort(port).withSsl(true).withVerifyPeer(false)
				.withPassword((CharSequence) password).build();
		RedisModulesClient client = RedisModulesClient.create(clientResources, redisURI);
		return client.connect();
	}

}
