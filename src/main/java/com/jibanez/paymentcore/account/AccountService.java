package com.jibanez.paymentcore.account;

import com.jibanez.paymentcore.account.dto.AccountResponse;
import com.jibanez.paymentcore.account.dto.BalanceResponse;
import com.jibanez.paymentcore.exception.BusinessExceptions;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNoGenerator accountNoGenerator;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountNoGenerator accountNoGenerator) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNoGenerator = accountNoGenerator;
    }

    @Transactional
    public AccountResponse create(String ownerUsername) {
        ApplicationUser owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(BusinessExceptions.InvalidCredentialsException::new);
        Account account = accountRepository.save(
                new Account(accountNoGenerator.next(), owner, "CLP"));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse findByAccountNo(String accountNo, String ownerUsername) {
        return toResponse(requireOwned(accountNo, ownerUsername));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNo, String ownerUsername) {
        Account account = requireOwned(accountNo, ownerUsername);
        return new BalanceResponse(account.getAccountNo(), account.getBalance());
    }

    private Account requireOwned(String accountNo, String ownerUsername) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(BusinessExceptions.AccountNotFoundException::new);
        if (!account.getOwner().getUsername().equals(ownerUsername)) {
            throw new BusinessExceptions.AccountAccessDeniedException();
        }
        return account;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getAccountNo(), account.getCurrency(),
                account.getBalance(), account.getStatus());
    }
}