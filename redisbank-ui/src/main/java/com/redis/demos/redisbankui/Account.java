package com.redis.demos.redisbankui;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RedisHash
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String iban;
    private String accountName;
    private Double balance;

}
