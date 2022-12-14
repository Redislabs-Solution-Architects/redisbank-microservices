package com.redis.demos.redisbanktransactions;

import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash
public class BankTransaction {

    private Long id;
    private String fromAccount;
    private String fromAccountName;
    private String toAccount;
    private String toAccountName;
    private String amount;
    private String description;
    private String transactionDate;
    private String transactionType;
    private String balanceAfter;

}
