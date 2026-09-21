package com.jibanez.paymentcore.exception;

import java.util.List;

public record ApiError(String code, String message, List<FieldErrorItem> fieldErrors) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}