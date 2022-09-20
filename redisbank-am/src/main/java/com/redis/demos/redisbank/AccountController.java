package com.redis.demos.redisbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;

@RestController
public class AccountController {
    
    private final StatefulRedisModulesConnection<String, String> srmc;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    public AccountController(StatefulRedisModulesConnection<String, String> srmc)  {
        this.srmc = srmc;
    }

    @GetMapping("/account")
    public Account getAccount(@RequestParam String iban) {
        try {
            String messageString = this.srmc.sync().jsonGet(iban);
            LOGGER.info("Message from Redis: {}", messageString);
            return SerializationUtil.deserializeObject(messageString, Account.class);
        } catch (JsonMappingException e) {
            LOGGER.error("Error mapping JSON: ", e.getMessage());
            return null;
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing JSON: ", e.getMessage());
            return null;
        }
    }

}
