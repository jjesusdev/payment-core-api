package com.jibanez.paymentcore.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.jibanez.paymentcore.BaseIntegrationTest;
import com.jibanez.paymentcore.account.dto.AccountResponse;
import com.jibanez.paymentcore.account.dto.BalanceResponse;
import com.jibanez.paymentcore.account.dto.DepositRequest;
import com.jibanez.paymentcore.auth.dto.AuthResponse;
import com.jibanez.paymentcore.auth.dto.RegisterRequest;
import com.jibanez.paymentcore.exception.ApiError;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AccountFlowIT extends BaseIntegrationTest {

    @Test
    void createDepositAndQueryBalance() {
        String token = register("dana");

        ResponseEntity<AccountResponse> created = rest.exchange(
                "/api/accounts", HttpMethod.POST, authedEntity(token), AccountResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accountNo = created.getBody().accountNo();
        assertThat(accountNo).hasSize(10);

        ResponseEntity<BalanceResponse> initial = rest.exchange(
                "/api/accounts/" + accountNo + "/balance", HttpMethod.GET,
                authedEntity(token), BalanceResponse.class);
        assertThat(initial.getBody().balance()).isEqualByComparingTo("0.00");

        ResponseEntity<TransactionResponse> deposit = rest.exchange(
                "/api/accounts/" + accountNo + "/deposits", HttpMethod.POST,
                authedBody(token, new DepositRequest(new BigDecimal("500.00"))),
                TransactionResponse.class);
        assertThat(deposit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(deposit.getBody().toAccountNo()).isEqualTo(accountNo);

        ResponseEntity<BalanceResponse> after = rest.exchange(
                "/api/accounts/" + accountNo + "/balance", HttpMethod.GET,
                authedEntity(token), BalanceResponse.class);
        assertThat(after.getBody().balance()).isEqualByComparingTo("500.00");
    }

    @Test
    void accountBelongsToCreatorOnly() {
        String owner = register("elena");
        String other = register("luis");
        String accountNo = rest.exchange("/api/accounts", HttpMethod.POST,
                authedEntity(owner), AccountResponse.class).getBody().accountNo();

        ResponseEntity<ApiError> denied = rest.exchange(
                "/api/accounts/" + accountNo, HttpMethod.GET,
                authedEntity(other), ApiError.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().code()).isEqualTo("ACCOUNT_ACCESS_DENIED");
    }

    @Test
    void depositRejectsInvalidAmount() {
        String token = register("marta");
        String accountNo = rest.exchange("/api/accounts", HttpMethod.POST,
                authedEntity(token), AccountResponse.class).getBody().accountNo();

        ResponseEntity<ApiError> response = rest.exchange(
                "/api/accounts/" + accountNo + "/deposits", HttpMethod.POST,
                authedBody(token, new DepositRequest(BigDecimal.ZERO)),
                ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String register(String username) {
        return rest.postForEntity("/api/auth/register",
                new RegisterRequest(username, "secreto123"), AuthResponse.class)
                .getBody().token();
    }

    private HttpEntity<Void> authedEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authedBody(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}