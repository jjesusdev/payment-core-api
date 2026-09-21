package com.jibanez.paymentcore.transaction;

import com.jibanez.paymentcore.account.Account;
import com.jibanez.paymentcore.account.AccountRepository;
import com.jibanez.paymentcore.account.AccountStatus;
import com.jibanez.paymentcore.exception.BusinessExceptions;
import com.jibanez.paymentcore.transaction.dto.TransactionResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository,
                              BankTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse deposit(String accountNo, String ownerUsername, BigDecimal amount) {
        validateAmount(amount);
        Account account = lockByAccountNo(accountNo);
        if (!account.getOwner().getUsername().equals(ownerUsername)) {
            throw new BusinessExceptions.AccountAccessDeniedException();
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessExceptions.AccountInactiveException();
        }
        account.setBalance(account.getBalance().add(amount));
        BankTransaction tx = transactionRepository.save(new BankTransaction(
                TransactionKind.DEPOSIT, amount, account.getCurrency(),
                UUID.randomUUID().toString(), null, account.getId()));
        return toResponse(tx, amount, account.getCurrency(), null, account.getAccountNo());
    }

    private Account lockByAccountNo(String accountNo) {
        Long id = accountRepository.findIdByAccountNo(accountNo)
                .orElseThrow(BusinessExceptions.AccountNotFoundException::new);
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(BusinessExceptions.AccountNotFoundException::new);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessExceptions.InvalidAmountException();
        }
    }

    private TransactionResponse toResponse(BankTransaction tx, BigDecimal amount,
                                           String currency, String fromAccountNo,
                                           String toAccountNo) {
        return new TransactionResponse(tx.getId(), tx.getKind().name(), amount,
                currency, fromAccountNo, toAccountNo);
    }
}