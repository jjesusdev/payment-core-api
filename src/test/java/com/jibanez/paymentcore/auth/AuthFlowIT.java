package com.jibanez.paymentcore.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.jibanez.paymentcore.BaseIntegrationTest;
import com.jibanez.paymentcore.auth.dto.AuthResponse;
import com.jibanez.paymentcore.auth.dto.LoginRequest;
import com.jibanez.paymentcore.auth.dto.RegisterRequest;
import com.jibanez.paymentcore.exception.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthFlowIT extends BaseIntegrationTest {

    @Test
    void registerAndLoginSucceed() {
        ResponseEntity<AuthResponse> registered = rest.postForEntity(
                "/api/auth/register",
                new RegisterRequest("ana", "secreto123"),
                AuthResponse.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody().token()).isNotBlank();
        assertThat(registered.getBody().username()).isEqualTo("ana");

        ResponseEntity<AuthResponse> loggedIn = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("ana", "secreto123"),
                AuthResponse.class);
        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loggedIn.getBody().token()).isNotBlank();
    }

    @Test
    void loginFailsWithWrongPassword() {
        rest.postForEntity("/api/auth/register",
                new RegisterRequest("bob", "secreto123"), AuthResponse.class);

        ResponseEntity<ApiError> response = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("bob", "mal-password"),
                ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        ResponseEntity<ApiError> response = rest.getForEntity(
                "/api/accounts", ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointAcceptsValidToken() {
        String token = rest.postForEntity("/api/auth/register",
                new RegisterRequest("carla", "secreto123"), AuthResponse.class).getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<ApiError> response = rest.exchange(
                "/api/accounts/999999", HttpMethod.GET,
                new HttpEntity<>(headers), ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}