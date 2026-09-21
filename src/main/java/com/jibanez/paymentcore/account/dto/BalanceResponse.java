package com.jibanez.paymentcore.account.dto;

import java.math.BigDecimal;

public record BalanceResponse(String accountNo, BigDecimal balance) {
}