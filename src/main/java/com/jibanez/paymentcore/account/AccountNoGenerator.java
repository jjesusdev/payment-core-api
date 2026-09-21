package com.jibanez.paymentcore.account;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class AccountNoGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountNoGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String next() {
        String candidate;
        do {
            candidate = "1" + String.format("%09d", random.nextInt(1_000_000_000));
        } while (accountRepository.existsByAccountNo(candidate));
        return candidate;
    }
}