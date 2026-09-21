package com.jibanez.paymentcore.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromAccountNo,
        @NotBlank String toAccountNo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}