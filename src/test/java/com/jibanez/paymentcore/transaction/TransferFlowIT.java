package com.jibanez.paymentcore.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.jibanez.paymentcore.BaseIntegrationTest;
import com.jibanez.paymentcore.account.dto.AccountResponse;
import com.jibanez.paymentcore.account.dto.BalanceResponse;
import com.jibanez.paymentcore.account.dto.DepositRequest;
import com.jibanez.paymentcore.auth.dto.AuthResponse;
import com.jibanez.paymentcore.auth.dto.RegisterRequest;
import com.jibanez.paymentcore.exception.ApiError;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import com.jibanez.paymentcore.transaction.dto.TransferRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TransferFlowIT extends BaseIntegrationTest {

    @Test
    void transferMovesMoneyBetweenTwoUsers() {
        String tokenA = register("userA");
        String tokenB = register("userB");
        String accA = createAccount(tokenA);
        String accB = createAccount(tokenB);
        deposit(tokenA, accA, "1000.00");

        ResponseEntity<TransactionResponse> transfer = transfer(tokenA, accA, accB, "300.00");

        assertThat(transfer.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transfer.getBody().fromAccountNo()).isEqualTo(accA);
        assertThat(transfer.getBody().toAccountNo()).isEqualTo(accB);
        assertThat(balance(tokenA, accA)).isEqualByComparingTo("700.00");
        assertThat(balance(tokenB, accB)).isEqualByComparingTo("300.00");
    }

    @Test
    void transferRejectsInsufficientBalanceWithoutMutating() {
        String tokenA = register("userC");
        String tokenB = register("userD");
        String accA = createAccount(tokenA);
        String accB = createAccount(tokenB);
        deposit(tokenA, accA, "50.00");

        ResponseEntity<ApiError> denied = transferError(tokenA, accA, accB, "100.00");

        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(denied.getBody().code()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(balance(tokenA, accA)).isEqualByComparingTo("50.00");
        assertThat(balance(tokenB, accB)).isEqualByComparingTo("0.00");
    }

    @Test
    void transferCannotMoveSomeoneElsesOrigin() {
        String tokenA = register("userE");
        String tokenB = register("userF");
        String accA = createAccount(tokenA);
        String accB = createAccount(tokenB);

        ResponseEntity<ApiError> denied = transferError(tokenB, accA, accB, "10.00");

        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().code()).isEqualTo("ACCOUNT_ACCESS_DENIED");
    }

    @Test
    void eightConcurrentTransfersPreserveBalanceWithLocking() throws Exception {
        String tokenA = register("userG");
        String tokenB = register("userH");
        String accA = createAccount(tokenA);
        String accB = createAccount(tokenB);
        deposit(tokenA, accA, "1000.00");

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Future<ResponseEntity<TransactionResponse>>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                try {
                    return transfer(tokenA, accA, accB, "100.00");
                } finally {
                    done.countDown();
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        for (Future<ResponseEntity<TransactionResponse>> f : futures) {
            assertThat(f.get().getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        assertThat(balance(tokenA, accA)).isEqualByComparingTo("200.00");
        assertThat(balance(tokenB, accB)).isEqualByComparingTo("800.00");
    }

    private String register(String username) {
        return rest.postForEntity("/api/auth/register",
                new RegisterRequest(username, "secreto123"), AuthResponse.class)
                .getBody().token();
    }

    private String createAccount(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<AccountResponse> response = rest.exchange(
                "/api/accounts", HttpMethod.POST, new HttpEntity<>(headers), AccountResponse.class);
        return response.getBody().accountNo();
    }

    private void deposit(String token, String accountNo, String amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        rest.exchange("/api/accounts/" + accountNo + "/deposits", HttpMethod.POST,
                new HttpEntity<>(new DepositRequest(new BigDecimal(amount)), headers),
                TransactionResponse.class);
    }

    private ResponseEntity<TransactionResponse> transfer(String token, String from, String to,
                                                         String amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/transfers", HttpMethod.POST,
                new HttpEntity<>(new TransferRequest(from, to, new BigDecimal(amount)), headers),
                TransactionResponse.class);
    }

    private ResponseEntity<ApiError> transferError(String token, String from, String to,
                                                   String amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/transfers", HttpMethod.POST,
                new HttpEntity<>(new TransferRequest(from, to, new BigDecimal(amount)), headers),
                ApiError.class);
    }

    private BigDecimal balance(String token, String accountNo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/accounts/" + accountNo + "/balance", HttpMethod.GET,
                new HttpEntity<>(headers), BalanceResponse.class).getBody().balance();
    }
}