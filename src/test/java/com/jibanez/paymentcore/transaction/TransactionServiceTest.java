package com.jibanez.paymentcore.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jibanez.paymentcore.account.Account;
import com.jibanez.paymentcore.account.AccountRepository;
import com.jibanez.paymentcore.exception.BusinessExceptions.AccountNotFoundException;
import com.jibanez.paymentcore.exception.BusinessExceptions.InsufficientBalanceException;
import com.jibanez.paymentcore.exception.BusinessExceptions.InvalidAmountException;
import com.jibanez.paymentcore.exception.BusinessExceptions.SameAccountException;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import com.jibanez.paymentcore.transaction.dto.TransferRequest;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRole;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    BankTransactionRepository transactionRepository;

    TransactionService service;

    Account from;
    Account to;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository);
        var owner = new ApplicationUser("pepita", "hash", UserRole.USER);
        from = new Account("1000000001", owner, "CLP");
        to = new Account("1000000002", owner, "CLP");
        from.setBalance(new BigDecimal("1000.00"));
        to.setBalance(new BigDecimal("0.00"));
        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubAccounts(Long fromId, Long toId) {
        when(accountRepository.findIdByAccountNo("1000000001")).thenReturn(Optional.of(fromId));
        when(accountRepository.findIdByAccountNo("1000000002")).thenReturn(Optional.of(toId));
        when(accountRepository.findByIdForUpdate(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(toId)).thenReturn(Optional.of(to));
        when(accountRepository.findByAccountNo(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNo("1000000001")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNo("1000000002")).thenReturn(Optional.of(to));
    }

    @Test
    void transfer_movesMoneyAtomically() {
        stubAccounts(1L, 2L); // id 1 < id 2 → se bloquea first=from, second=to

        TransactionResponse response = service.transfer(
                new TransferRequest("1000000001", "1000000002", new BigDecimal("300.00")),
                "pepita");

        assertThat(response.amount()).isEqualByComparingTo("300.00");
        assertThat(from.getBalance()).isEqualByComparingTo("700.00");
        assertThat(to.getBalance()).isEqualByComparingTo("300.00");
        verify(transactionRepository).save(any(BankTransaction.class));
    }

    @Test
    void transfer_movesMoneyWhenIdsAreReversed() {
        stubAccounts(2L, 1L); // id 2 > id 1 → se bloquea first=to, second=from

        service.transfer(new TransferRequest("1000000001", "1000000002",
                new BigDecimal("100.00")), "pepita");

        assertThat(from.getBalance()).isEqualByComparingTo("900.00");
        assertThat(to.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_rejectsSameAccount() {
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("1000000001", "1000000001", new BigDecimal("10.00")),
                "pepita"))
                .isInstanceOf(SameAccountException.class);
    }

    @Test
    void transfer_rejectsInsufficientBalance() {
        stubAccounts(1L, 2L);
        from.setBalance(new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("1000000001", "1000000002", new BigDecimal("100.00")),
                "pepita"))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(from.getBalance()).isEqualByComparingTo("50.00");
        assertThat(to.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_rejectsWhenOriginNotFound() {
        stubAccounts(1L, 2L);
        when(accountRepository.findByAccountNo("1000000001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("1000000001", "1000000002", new BigDecimal("100.00")),
                "pepita"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("1000000001", "1000000002", BigDecimal.ZERO),
                "pepita"))
                .isInstanceOf(InvalidAmountException.class);
    }
}