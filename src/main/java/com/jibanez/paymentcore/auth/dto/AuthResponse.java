package com.jibanez.paymentcore.auth.dto;

import com.jibanez.paymentcore.user.UserRole;

public record AuthResponse(String token, String username, UserRole role) {
}