package com.redis.demos.redisbank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {
    
    private final AccountRepository ar;

    public AccountController(AccountRepository ar)  {
        this.ar = ar;
    }

    @GetMapping("/account")
    public Account getAccount(@RequestParam String iban) {
        return this.ar.findById(iban).orElseThrow();
    }

}
