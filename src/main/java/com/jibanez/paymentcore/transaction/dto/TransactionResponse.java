package com.jibanez.paymentcore.transaction.dto;

import java.math.BigDecimal;

public record TransactionResponse(Long id, String kind, BigDecimal amount, String currency,
                                  String fromAccountNo, String toAccountNo) {
}