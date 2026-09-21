package com.jibanez.paymentcore.account;

import com.jibanez.paymentcore.account.dto.AccountResponse;
import com.jibanez.paymentcore.account.dto.BalanceResponse;
import com.jibanez.paymentcore.account.dto.DepositRequest;
import com.jibanez.paymentcore.auth.SecurityUtils;
import com.jibanez.paymentcore.transaction.TransactionService;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService,
                             TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(SecurityUtils.currentUsername()));
    }

    @GetMapping("/{accountNo}")
    public AccountResponse get(@PathVariable String accountNo) {
        return accountService.findByAccountNo(accountNo, SecurityUtils.currentUsername());
    }

    @GetMapping("/{accountNo}/balance")
    public BalanceResponse balance(@PathVariable String accountNo) {
        return accountService.getBalance(accountNo, SecurityUtils.currentUsername());
    }

    @PostMapping("/{accountNo}/deposits")
    public ResponseEntity<TransactionResponse> deposit(@PathVariable String accountNo,
                                                       @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(
                accountNo, SecurityUtils.currentUsername(), request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}