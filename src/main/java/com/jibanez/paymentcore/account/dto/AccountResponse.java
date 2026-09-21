package com.jibanez.paymentcore.account.dto;

import com.jibanez.paymentcore.account.AccountStatus;
import java.math.BigDecimal;

public record AccountResponse(String accountNo, String currency,
                              BigDecimal balance, AccountStatus status) {
}