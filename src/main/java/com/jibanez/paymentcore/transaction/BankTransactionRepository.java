package com.jibanez.paymentcore.transaction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByFromAccountIdOrToAccountIdOrderByExecutedAtDesc(
            Long fromAccountId, Long toAccountId);
}