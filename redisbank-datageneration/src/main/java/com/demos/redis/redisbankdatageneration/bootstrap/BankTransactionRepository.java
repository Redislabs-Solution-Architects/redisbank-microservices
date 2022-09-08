package com.demos.redis.redisbankdatageneration.bootstrap;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.demos.redis.redisbankdatageneration.domain.BankTransaction;

@Repository
public interface BankTransactionRepository extends CrudRepository<BankTransaction, String>{
    
}