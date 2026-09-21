package com.jibanez.paymentcore.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jibanez.paymentcore.account.dto.AccountResponse;
import com.jibanez.paymentcore.exception.BusinessExceptions.AccountAccessDeniedException;
import com.jibanez.paymentcore.user.ApplicationUser;
import com.jibanez.paymentcore.user.UserRepository;
import com.jibanez.paymentcore.user.UserRole;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    UserRepository userRepository;

    AccountNoGenerator generator;

    AccountService service;

    @BeforeEach
    void setUp() {
        generator = new AccountNoGenerator(accountRepository);
        service = new AccountService(accountRepository, userRepository, generator);
    }

    @Test
    void create_assignsUniqueNumberAndZeroBalance() {
        var pepita = new ApplicationUser("pepita", "hash", UserRole.USER);
        when(userRepository.findByUsername("pepita")).thenReturn(Optional.of(pepita));
        when(accountRepository.existsByAccountNo(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = service.create("pepita");

        assertThat(response.accountNo()).hasSize(10).startsWith("1");
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void findByAccountNo_deniesForeignOwner() {
        var other = new ApplicationUser("juan", "hash", UserRole.USER);
        Account foreign = new Account("1000000002", other, "CLP");
        when(accountRepository.findByAccountNo("1000000002")).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.findByAccountNo("1000000002", "pepita"))
                .isInstanceOf(AccountAccessDeniedException.class);
    }
}